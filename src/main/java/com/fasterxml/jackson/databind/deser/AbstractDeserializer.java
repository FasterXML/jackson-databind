package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.ObjectIdResolver;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Deserializer only used for abstract types used as placeholders during polymorphic
 * type handling deserialization. If so, there is no real deserializer associated
 * with nominal type, just {@link TypeDeserializer}; and any calls that do not
 * pass such resolver will result in an error.
 */
public class AbstractDeserializer
    extends JsonDeserializer<Object>
    implements ContextualDeserializer, // since 2.9
        java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final JavaType _baseType;

    protected final ObjectIdReader _objectIdReader;

    protected final Map<String, SettableBeanProperty> _backRefProperties;

    // support for "native" types, which require special care:
    
    protected final boolean _acceptString;
    protected final boolean _acceptBoolean;
    protected final boolean _acceptInt;
    protected final boolean _acceptDouble;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    public AbstractDeserializer(BeanDeserializerBuilder builder,
            BeanDescription beanDesc, Map<String, SettableBeanProperty> backRefProps)
    {
        _baseType = beanDesc.getType();
        _objectIdReader = builder.getObjectIdReader();
        _backRefProperties = backRefProps;
        Class<?> cls = _baseType.getRawClass();
        _acceptString = cls.isAssignableFrom(String.class);
        _acceptBoolean = (cls == Boolean.TYPE) || cls.isAssignableFrom(Boolean.class);
        _acceptInt = (cls == Integer.TYPE) || cls.isAssignableFrom(Integer.class);
        _acceptDouble = (cls == Double.TYPE) || cls.isAssignableFrom(Double.class);
    }

    protected AbstractDeserializer(BeanDescription beanDesc)
    {
        _baseType = beanDesc.getType();
        _objectIdReader = null;
        _backRefProperties = null;
        Class<?> cls = _baseType.getRawClass();
        _acceptString = cls.isAssignableFrom(String.class);
        _acceptBoolean = (cls == Boolean.TYPE) || cls.isAssignableFrom(Boolean.class);
        _acceptInt = (cls == Integer.TYPE) || cls.isAssignableFrom(Integer.class);
        _acceptDouble = (cls == Double.TYPE) || cls.isAssignableFrom(Double.class);
    }

    /**
     * @since 2.9
     */
    protected AbstractDeserializer(AbstractDeserializer base,
            ObjectIdReader objectIdReader)
    {
        _baseType = base._baseType;
        _backRefProperties = base._backRefProperties;
        _acceptString = base._acceptString;
        _acceptBoolean = base._acceptBoolean;
        _acceptInt = base._acceptInt;
        _acceptDouble = base._acceptDouble;

        _objectIdReader = objectIdReader;
    }
    
    /**
     * Factory method used when constructing instances for non-POJO types, like
     * {@link java.util.Map}s.
     * 
     * @since 2.3
     */
    public static AbstractDeserializer constructForNonPOJO(BeanDescription beanDesc) {
        return new AbstractDeserializer(beanDesc);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        // First: may have an override for Object Id:
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        final AnnotatedMember accessor = (property == null || intr == null)
                ? null : property.getMember();
        if (accessor != null && intr != null) {
            ObjectIdInfo objectIdInfo = intr.findObjectIdInfo(accessor);
            if (objectIdInfo != null) { // some code duplication here as well (from BeanDeserializerFactory)
                // 2.1: allow modifications by "id ref" annotations as well:
                objectIdInfo = intr.findObjectReferenceInfo(accessor, objectIdInfo);
                
                Class<?> implClass = objectIdInfo.getGeneratorType();
                // 02-May-2017, tatu: Alas, properties are NOT available for abstract classes; can not
                //    support this particular type
                if (implClass == ObjectIdGenerators.PropertyGenerator.class) {
                    ctxt.reportMappingException(
"Invalid Object Id definition for abstract type %s: can not use `PropertyGenerator` on polymorphic types using property annotation",
handledType().getName());
                }
                ObjectIdResolver resolver = ctxt.objectIdResolverInstance(accessor, objectIdInfo);
                JavaType type = ctxt.constructType(implClass);
                JavaType idType = ctxt.getTypeFactory().findTypeParameters(type, ObjectIdGenerator.class)[0];
                SettableBeanProperty idProp = null;
                ObjectIdGenerator<?> idGen = ctxt.objectIdGeneratorInstance(accessor, objectIdInfo);
                JsonDeserializer<?> deser = ctxt.findRootValueDeserializer(idType);
                ObjectIdReader oir = ObjectIdReader.construct(idType, objectIdInfo.getPropertyName(),
                         idGen, deser, idProp, resolver);
                return new AbstractDeserializer(this, oir);
            }
        }
        // either way, need to resolve serializer:
        return this;
    }

    /*
    /**********************************************************
    /* Public accessors
    /**********************************************************
     */

    @Override
    public Class<?> handledType() {
        return _baseType.getRawClass();
    }
    
    @Override
    public boolean isCachable() { return true; }
    
    /**
     * Overridden to return true for those instances that are
     * handling value for which Object Identity handling is enabled
     * (either via value type or referring property).
     */
    @Override
    public ObjectIdReader getObjectIdReader() {
        return _objectIdReader;
    }

    /**
     * Method called by <code>BeanDeserializer</code> to resolve back reference
     * part of managed references.
     */
    @Override
    public SettableBeanProperty findBackReference(String logicalName) {
        return (_backRefProperties == null) ? null : _backRefProperties.get(logicalName);
    }
    
    /*
    /**********************************************************
    /* Deserializer implementation
    /**********************************************************
     */
    
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException
    {
        // Hmmh. One tricky question; for scalar, is it an Object Id, or "Natural" type?
        // for now, prefer Object Id:
        if (_objectIdReader != null) {
            JsonToken t = p.getCurrentToken();
            if (t != null) {
                // Most commonly, a scalar (int id, uuid String, ...)
                if (t.isScalarValue()) {
                    return _deserializeFromObjectId(p, ctxt);
                }
                // but, with 2.5+, a simple Object-wrapped value also legal:
                if (t == JsonToken.START_OBJECT) {
                    t = p.nextToken();
                }
                if ((t == JsonToken.FIELD_NAME) && _objectIdReader.maySerializeAsObject()
                        && _objectIdReader.isValidReferencePropertyName(p.getCurrentName(), p)) {
                    return _deserializeFromObjectId(p, ctxt);
                }
            
            }
        }
        
        // First: support "natural" values (which are always serialized without type info!)
        Object result = _deserializeIfNatural(p, ctxt);
        if (result != null) {
            return result;
        }
        return typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        return ctxt.handleMissingInstantiator(_baseType.getRawClass(), p,
                "abstract types either need to be mapped to concrete types, have custom deserializer, or contain additional type information");
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected Object _deserializeIfNatural(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        /* There is a chance we might be "natural" types
         * (String, Boolean, Integer, Double), which do not include any type information...
         * Care must be taken to only return this if return type matches, however.
         * Finally, we may have to consider possibility of custom handlers for
         * these values: but for now this should work ok.
         */
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_STRING:
            if (_acceptString) {
                return p.getText();
            }
            break;
        case JsonTokenId.ID_NUMBER_INT:
            if (_acceptInt) {
                return p.getIntValue();
            }
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (_acceptDouble) {
                return Double.valueOf(p.getDoubleValue());
            }
            break;
        case JsonTokenId.ID_TRUE:
            if (_acceptBoolean) {
                return Boolean.TRUE;
            }
            break;
        case JsonTokenId.ID_FALSE:
            if (_acceptBoolean) {
                return Boolean.FALSE;
            }
            break;
        }
        return null;
    }

    /**
     * Method called in cases where it looks like we got an Object Id
     * to parse and use as a reference.
     */
    protected Object _deserializeFromObjectId(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        Object id = _objectIdReader.readObjectReference(p, ctxt);
        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator, _objectIdReader.resolver);
        // do we have it resolved?
        Object pojo = roid.resolve();
        if (pojo == null) { // not yet; should wait...
            throw new UnresolvedForwardReference(p,
                    "Could not resolve Object Id ["+id+"] -- unresolved forward-reference?", p.getCurrentLocation(), roid);
        }
        return pojo;
    }
}

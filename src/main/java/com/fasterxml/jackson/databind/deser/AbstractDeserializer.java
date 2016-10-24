package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.ReadableObjectId;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Deserializer only used for abstract types used as placeholders during polymorphic
 * type handling deserialization. If so, there is no real deserializer associated
 * with nominal type, just {@link TypeDeserializer}; and any calls that do not
 * pass such resolver will result in an error.
 */
public class AbstractDeserializer
    extends JsonDeserializer<Object>
    implements java.io.Serializable
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
     * Factory method used when constructing instances for non-POJO types, like
     * {@link java.util.Map}s.
     * 
     * @since 2.3
     */
    public static AbstractDeserializer constructForNonPOJO(BeanDescription beanDesc) {
        return new AbstractDeserializer(beanDesc);
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

    @Override // since 2.9
    public Boolean supportsUpdate(DeserializationConfig config) {
        /* 23-Oct-2016, tatu: Not exactly sure what to do with this; polymorphic
         *   type handling seems bit risky so for now claim it "may or may not be"
         *   possible, which does allow explicit per-type/per-property merging attempts,
         *   but avoids general-configuration merges
         */
        return null;
    }

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
        // 16-Oct-2016, tatu: Let's pass non-null value instantiator so that we will
        //    get proper exception type; needed to establish there are no creators
        //    (since without ValueInstantiator this would not be known for certain)
        ValueInstantiator bogus = new ValueInstantiator.Base(_baseType);
        return ctxt.handleMissingInstantiator(_baseType.getRawClass(), bogus, p,
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

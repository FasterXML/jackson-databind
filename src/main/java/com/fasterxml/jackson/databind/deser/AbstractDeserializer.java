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
    private static final long serialVersionUID = -3010349050434697698L;

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
    public static AbstractDeserializer constructForNonPOJO(BeanDescription beanDesc)
    {
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
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        // Hmmh. One tricky question; for scalar, is it an Object Id, or "Natural" type?
        // for now, prefer Object Id:
        if (_objectIdReader != null) {
            JsonToken t = jp.getCurrentToken();
            // should be good enough check; we only care about Strings, integral numbers:
            if (t != null && t.isScalarValue()) {
                return _deserializeFromObjectId(jp, ctxt);
            }
        }
        
        // First: support "natural" values (which are always serialized without type info!)
        Object result = _deserializeIfNatural(jp, ctxt);
        if (result != null) {
            return result;
        }
        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        // This method should never be called...
        throw ctxt.instantiationException(_baseType.getRawClass(),
                "abstract types either need to be mapped to concrete types, have custom deserializer, or be instantiated with additional type information");
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    protected Object _deserializeIfNatural(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* As per [JACKSON-417], there is a chance we might be "natural" types
         * (String, Boolean, Integer, Double), which do not include any type information...
         * Care must be taken to only return this if return type matches, however.
         * Finally, we may have to consider possibility of custom handlers for
         * these values: but for now this should work ok.
         */
        /* 21-Sep-2013, tatu: It may seem odd that I'm not using a switch here.
         *   But turns out that a switch on an enum generates an inner class...
         *   crazy! So this is to avoid that, simply since new class weighs about 1kB
         *   after compression.
         */
        final JsonToken t = jp.getCurrentToken();
        if (t.isScalarValue()) {
            if (t == JsonToken.VALUE_STRING) {
                if (_acceptString) {
                    return jp.getText();
                }
            } else if (t == JsonToken.VALUE_NUMBER_INT) {
                if (_acceptInt) {
                    return jp.getIntValue();
                }
            } else if (t == JsonToken.VALUE_NUMBER_FLOAT) {
                if (_acceptDouble) {
                    return Double.valueOf(jp.getDoubleValue());
                }
            } else if (t == JsonToken.VALUE_TRUE) {
                if (_acceptBoolean) {
                    return Boolean.TRUE;
                }
            } else if (t == JsonToken.VALUE_FALSE) {
                if (_acceptBoolean) {
                    return Boolean.FALSE;
                }
            }
        }
        return null;
    }

    /**
     * Method called in cases where it looks like we got an Object Id
     * to parse and use as a reference.
     */
    protected Object _deserializeFromObjectId(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Object id = _objectIdReader.readObjectReference(jp, ctxt);
        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator, _objectIdReader.resolver);
        // do we have it resolved?
        Object pojo = roid.resolve();
        if (pojo == null) { // not yet; should wait...
            throw new IllegalStateException("Could not resolve Object Id ["+id+"] -- unresolved forward-reference?");
        }
        return pojo;
    }
}


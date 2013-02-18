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
 * 
 * @author tatu
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

    /*
    /**********************************************************
    /* Public accessors
    /**********************************************************
     */

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
    public SettableBeanProperty findBackReference(String logicalName)
    {
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
    
    @SuppressWarnings("incomplete-switch")
    protected Object _deserializeIfNatural(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* As per [JACKSON-417], there is a chance we might be "natural" types
         * (String, Boolean, Integer, Double), which do not include any type information...
         * Care must be taken to only return this if return type matches, however.
         * Finally, we may have to consider possibility of custom handlers for
         * these values: but for now this should work ok.
         */
        switch (jp.getCurrentToken()) {
        case VALUE_STRING:
            if (_acceptString) {
                return jp.getText();
            }
            break;
        case VALUE_NUMBER_INT:
            if (_acceptInt) {
                return jp.getIntValue();
            }
            break;

        case VALUE_NUMBER_FLOAT:
            if (_acceptDouble) {
                return Double.valueOf(jp.getDoubleValue());
            }
            break;
        case VALUE_TRUE:
            if (_acceptBoolean) {
                return Boolean.TRUE;
            }
            break;
        case VALUE_FALSE:
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
    protected Object _deserializeFromObjectId(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Object id = _objectIdReader.deserializer.deserialize(jp, ctxt);
        ReadableObjectId roid = ctxt.findObjectId(id, _objectIdReader.generator);
        // do we have it resolved?
        Object pojo = roid.item;
        if (pojo == null) { // not yet; should wait...
            throw new IllegalStateException("Could not resolve Object Id ["+id+"] -- unresolved forward-reference?");
        }
        return pojo;
    }
}


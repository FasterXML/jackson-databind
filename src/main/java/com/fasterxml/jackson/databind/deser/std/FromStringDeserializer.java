package com.fasterxml.jackson.databind.deser.std;

import java.io.*;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * Base class for simple deserializers that only accept JSON String
 * values as the source.
 */
public abstract class FromStringDeserializer<T>
    extends StdScalarDeserializer<T>
{
    private static final long serialVersionUID = 1L;

    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }

    /*
    /**********************************************************
    /* Deserializer implementations
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    @Override
    public final T deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // 22-Sep-2012, tatu: For 2.1, use this new method, may force coercion:
        String text = jp.getValueAsString();
        if (text != null) { // has String representation
            if (text.length() == 0 || (text = text.trim()).length() == 0) {
                // 15-Oct-2010, tatu: Empty String usually means null, so
                return null;
            }
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
                // nothing to do here, yet? We'll fail anyway
            }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid textual representation");
        }
        if (jp.getCurrentToken() == JsonToken.VALUE_EMBEDDED_OBJECT) {
            // Trivial cases; null to null, instance of type itself returned as is
            Object ob = jp.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (_valueClass.isAssignableFrom(ob.getClass())) {
                return (T) ob;
            }
            return _deserializeEmbedded(ob, ctxt);
        }
        throw ctxt.mappingException(_valueClass);
    }
        
    protected abstract T _deserialize(String value, DeserializationContext ctxt)
        throws IOException, JsonProcessingException;

    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // default impl: error out
        throw ctxt.mappingException("Don't know how to convert embedded Object of type "
                +ob.getClass().getName()+" into "+_valueClass.getName());
    }

}

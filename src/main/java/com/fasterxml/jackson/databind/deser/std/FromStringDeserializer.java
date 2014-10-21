package com.fasterxml.jackson.databind.deser.std;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;

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
        throws IOException
    {
        // 22-Sep-2012, tatu: For 2.1, use this new method, may force coercion:
        String text = jp.getValueAsString();
        if (text != null) { // has String representation
            if (text.length() == 0 || (text = text.trim()).length() == 0) {
                return _deserializeFromEmptyString();
            }
            Exception cause = null;
            try {
                T result = _deserialize(text, ctxt);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException iae) {
                cause = iae;
            }
            String msg = "not a valid textual representation";
            if (cause != null) {
                 String m2 = cause.getMessage();
                 if (m2 != null) {
                     msg = msg +", problem: "+ m2;
                 }
            }
            JsonMappingException e = ctxt.weirdStringException(text, _valueClass, msg);
            if (cause != null) {
                e.initCause(cause);
            }
            throw e;
            // nothing to do here, yet? We'll fail anyway
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
        throws IOException;

    protected T _deserializeEmbedded(Object ob, DeserializationContext ctxt)
        throws IOException
    {
        // default impl: error out
        throw ctxt.mappingException("Don't know how to convert embedded Object of type "
                +ob.getClass().getName()+" into "+_valueClass.getName());
    }

    protected T _deserializeFromEmptyString() { return null; }
}

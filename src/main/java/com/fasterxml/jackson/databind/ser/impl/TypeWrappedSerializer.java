package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Simple serializer that will call configured type serializer, passing
 * in configured data serializer, and exposing it all as a simple
 * serializer.
 */
public final class TypeWrappedSerializer
    extends JsonSerializer<Object>
{
    final protected TypeSerializer _typeSerializer;
    final protected JsonSerializer<Object> _serializer;

    @SuppressWarnings("unchecked")
    public TypeWrappedSerializer(TypeSerializer typeSer, JsonSerializer<?> ser)
    {
        super();
        _typeSerializer = typeSer;
        _serializer = (JsonSerializer<Object>) ser;
    }

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        _serializer.serializeWithType(value, jgen, provider, _typeSerializer);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        /* Is this an erroneous call? For now, let's assume it is not, and
         * that type serializer is just overridden if so
         */
        _serializer.serializeWithType(value, jgen, provider, typeSer);
    }
    
    @Override
    public Class<Object> handledType() { return Object.class; }

    /*
    /**********************************************************
    /* Extended API for other core classes
    /**********************************************************
     */

    public JsonSerializer<Object> valueSerializer() {
        return _serializer;
    }

    public TypeSerializer typeSerializer() {
        return _typeSerializer;
    }
}

package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Simple serializer that will call configured type serializer, passing
 * in configured data serializer, and exposing it all as a simple
 * serializer.
 */
public final class TypeWrappedSerializer
    extends JsonSerializer<Object>
    implements ContextualSerializer // since 2.9
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
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
        _serializer.serializeWithType(value, g, provider, _typeSerializer);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        // Is this an erroneous call? For now, let's assume it is not, and
        // that type serializer is just overridden if so
        _serializer.serializeWithType(value, g, provider, typeSer);
    }

    @Override
    public Class<Object> handledType() { return Object.class; }

    /*
    /**********************************************************
    /* ContextualDeserializer
    /**********************************************************
     */

    @Override // since 2.9
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
        throws JsonMappingException
    {
        // 13-Mar-2017, tatu: Should we call `TypeSerializer.forProperty()`?
        JsonSerializer<?> ser = _serializer;
        if (ser instanceof ContextualSerializer) {
            ser = provider.handleSecondaryContextualization(ser, property);
        }
        if (ser == _serializer) {
            return this;
        }
        return new TypeWrappedSerializer(_typeSerializer, ser);
    }

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

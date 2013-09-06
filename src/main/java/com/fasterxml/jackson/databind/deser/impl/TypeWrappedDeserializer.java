package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Simple deserializer that will call configured type deserializer, passing
 * in configured data deserializer, and exposing it all as a simple
 * deserializer.
 * This is necessary when there is no "parent" deserializer which could handle
 * details of calling a {@link TypeDeserializer}, most commonly used with
 * root values.
 */
public final class TypeWrappedDeserializer
    extends JsonDeserializer<Object>
{
    final TypeDeserializer _typeDeserializer;
    final JsonDeserializer<Object> _deserializer;

    public TypeWrappedDeserializer(TypeDeserializer typeDeser, JsonDeserializer<Object> deser)
    {
        super();
        _typeDeserializer = typeDeser;
        _deserializer = deser;
    }

    @Override
    public Class<?> handledType() {
        return _deserializer.handledType();
    }
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        return _deserializer.deserializeWithType(jp, ctxt, _typeDeserializer);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
        TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
    {
        // should never happen? (if it can, could call on that object)
        throw new IllegalStateException("Type-wrapped deserializer's deserializeWithType should never get called");
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt,
            Object intoValue)
        throws IOException, JsonProcessingException
    {
        /* 01-Mar-2013, tatu: Hmmh. Tough call as to what to do... need
         *   to delegate, but will this work reliably? Let's just hope so:
         */
        return _deserializer.deserialize(jp,  ctxt, intoValue);
    }
}
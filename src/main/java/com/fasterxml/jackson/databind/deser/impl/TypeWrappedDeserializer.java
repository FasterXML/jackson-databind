package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
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
    implements java.io.Serializable // since 2.5
{
    private static final long serialVersionUID = 1L;

    final protected TypeDeserializer _typeDeserializer;
    final protected JsonDeserializer<Object> _deserializer;

    @SuppressWarnings("unchecked")
    public TypeWrappedDeserializer(TypeDeserializer typeDeser, JsonDeserializer<?> deser)
    {
        super();
        _typeDeserializer = typeDeser;
        _deserializer = (JsonDeserializer<Object>) deser;
    }

    @Override
    public Class<?> handledType() {
        return _deserializer.handledType();
    }
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        return _deserializer.deserializeWithType(jp, ctxt, _typeDeserializer);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
        TypeDeserializer typeDeserializer) throws IOException
    {
        // should never happen? (if it can, could call on that object)
        throw new IllegalStateException("Type-wrapped deserializer's deserializeWithType should never get called");
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt,
            Object intoValue) throws IOException
    {
        /* 01-Mar-2013, tatu: Hmmh. Tough call as to what to do... need
         *   to delegate, but will this work reliably? Let's just hope so:
         */
        return _deserializer.deserialize(jp,  ctxt, intoValue);
    }
}
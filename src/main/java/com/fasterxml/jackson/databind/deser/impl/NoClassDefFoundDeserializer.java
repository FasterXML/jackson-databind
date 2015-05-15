package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * A deserializer that stores a {@link NoClassDefFoundError} error
 * and throws the stored exception when attempting to deserialize
 * a value. Null and empty values can be deserialized without error.
 */
public class NoClassDefFoundDeserializer<T> extends JsonDeserializer<T>
{
    private final NoClassDefFoundError _cause;

    public NoClassDefFoundDeserializer(NoClassDefFoundError cause)
    {
        _cause = cause;
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        throw _cause;
    }
}

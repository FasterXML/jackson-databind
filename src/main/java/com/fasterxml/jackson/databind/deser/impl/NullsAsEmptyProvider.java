package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will return "empty value"
 * specified by {@link JsonDeserializer} provider is constructed with.
 */
public class NullsAsEmptyProvider
    implements NullValueProvider, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final JsonDeserializer<?> _deserializer;

    public NullsAsEmptyProvider(JsonDeserializer<?> deser) {
        _deserializer = deser;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt)
            throws JsonMappingException {
        return _deserializer.getEmptyValue(ctxt);
    }
}

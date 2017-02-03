package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will always throw a
 * {@link InvalidNullException} when a null is encountered.
 */
public class NullsFailProvider implements NullValueProvider
{
    protected final PropertyName _name;
    protected final JavaType _type;

    public NullsFailProvider(PropertyName name, JavaType type) {
        _name = name;
        _type = type;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        // Must be called every time to effect the exception...
        return AccessPattern.DYNAMIC;
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt)
            throws JsonMappingException {
        throw InvalidNullException.from(ctxt, _name, _type);
    }
}

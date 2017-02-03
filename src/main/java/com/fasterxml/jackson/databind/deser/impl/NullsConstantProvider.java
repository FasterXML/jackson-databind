package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will always throw a
 * {@link InvalidNullException} when a null is encountered.
 */
public class NullsConstantProvider implements NullValueProvider
{
    private final static NullsConstantProvider SKIPPER = new NullsConstantProvider(NullValueProvider.SKIP_MARKER);

    private final static NullsConstantProvider NULLER = new NullsConstantProvider(null);
    
    protected final Object _nullValue;

    protected final AccessPattern _access;

    public NullsConstantProvider(Object nvl) {
        _nullValue = nvl;
        _access = (_nullValue == null) ? AccessPattern.ALWAYS_NULL
                : AccessPattern.CONSTANT;
    }

    /**
     * Static accessor for a stateless instance that always returns
     * {@link NullValueProvider#SKIP_MARKER} marker instance, used to indicate
     * that null value is to be skipped instead of replacing it.
     */
    public static NullsConstantProvider skipper() {
        return SKIPPER;
    }

    public static NullsConstantProvider nuller() {
        return NULLER;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return _access;
    }
    
    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return _nullValue;
    }
}

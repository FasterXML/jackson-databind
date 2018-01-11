package com.fasterxml.jackson.databind.util;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;

/**
 * Trivial {@link ValueInstantiator} implementation that will simply return constant
 * {@code Object} it is configured with. May be used as-is, or as base class to override
 * simplistic behavior further.
 */
public class ConstantValueInstantiator extends ValueInstantiator
{
    protected final Object _value;

    public ConstantValueInstantiator(Object value) {
        _value = value;
    }

    @Override
    public Class<?> getValueClass() {
        return _value.getClass();
    }

    @Override // yes, since default ctor works
    public boolean canInstantiate() { return true; }

    @Override
    public boolean canCreateUsingDefault() {  return true; }

    @Override
    public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
        return _value;
    }

    @Override
    public String getValueTypeDesc() {
        return _value.getClass().getName();
    }
}

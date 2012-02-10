package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;

/**
 * Container object that encapsulates information usually
 * derived from {@link JsonIdentityInfo} annotation or its
 * custom alternatives
 */
public class ObjectIdInfo
{
    protected final String _propertyName;
    protected final Class<? extends ObjectIdGenerator<?>> _generator;
    protected final Class<?> _scope;
    
    public ObjectIdInfo(String prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen)
    {
        _propertyName = prop;
        _generator = gen;
        _scope = scope;
    }

    public String getPropertyName() { return _propertyName; }
    public Class<?> getScope() { return _scope; }
    public Class<? extends ObjectIdGenerator<?>> getGenerator() { return _generator; }
}
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
    protected final String _property;
    protected final Class<? extends ObjectIdGenerator<?>> _generator;
    
    public ObjectIdInfo(String prop, Class<? extends ObjectIdGenerator<?>> gen)
    {
        _property = prop;
        _generator = gen;
    }

    public String getProperty() { return _property; }
    public Class<? extends ObjectIdGenerator<?>> getGenerator() { return _generator; }
}
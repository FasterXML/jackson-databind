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
    protected final boolean _firstAsId;

    /**
     * @deprecated Since 2.1 use the constructor that takes 4 arguments.
     */
    @Deprecated
    public ObjectIdInfo(String prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen) {
        this(prop, scope, gen, false);
    }
    
    public ObjectIdInfo(String prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen,
            boolean firstAsId)
    {
        _propertyName = prop;
        _generator = gen;
        _scope = scope;
        _firstAsId = firstAsId;
    }

    public String getPropertyName() { return _propertyName; }
    public Class<?> getScope() { return _scope; }
    public Class<? extends ObjectIdGenerator<?>> getGeneratorType() { return _generator; }
    public boolean getFirstAsId() { return _firstAsId; }
}
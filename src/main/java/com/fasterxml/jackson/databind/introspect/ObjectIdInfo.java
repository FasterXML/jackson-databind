package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.databind.PropertyName;

/**
 * Container object that encapsulates information usually
 * derived from {@link JsonIdentityInfo} annotation or its
 * custom alternatives
 */
public class ObjectIdInfo
{
    protected final PropertyName _propertyName;
    protected final Class<? extends ObjectIdGenerator<?>> _generator;
    protected final Class<?> _scope;
    protected final boolean _alwaysAsId;

    public ObjectIdInfo(PropertyName name, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen) {
        this(name, scope, gen, false);
    }

    @Deprecated // since 2.3
    public ObjectIdInfo(String name, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen) {
        this(new PropertyName(name), scope, gen, false);
    }
    
    protected ObjectIdInfo(PropertyName prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen,
            boolean alwaysAsId)
    {
        _propertyName = prop;
        _scope = scope;
        _generator = gen;
        _alwaysAsId = alwaysAsId;
    }

    public ObjectIdInfo withAlwaysAsId(boolean state) {
        if (_alwaysAsId == state) {
            return this;
        }
        return new ObjectIdInfo(_propertyName, _scope, _generator, state);
    }
    
    public PropertyName getPropertyName() { return _propertyName; }
    public Class<?> getScope() { return _scope; }
    public Class<? extends ObjectIdGenerator<?>> getGeneratorType() { return _generator; }
    public boolean getAlwaysAsId() { return _alwaysAsId; }

    @Override
    public String toString() {
        return "ObjectIdInfo: propName="+_propertyName
                +", scope="+(_scope == null ? "null" : _scope.getName())
                +", generatorType="+(_generator == null ? "null" : _generator.getName())
                +", alwaysAsId="+_alwaysAsId;
    }
}

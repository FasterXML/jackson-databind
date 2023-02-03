package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Container object that encapsulates information usually
 * derived from {@link JsonIdentityInfo} annotation or its
 * custom alternatives
 */
public class ObjectIdInfo
{
    protected final PropertyName _propertyName;
    protected final Class<? extends ObjectIdGenerator<?>> _generator;
    protected final Class<? extends ObjectIdResolver> _resolver;
    protected final Class<?> _scope;
    protected final boolean _alwaysAsId;

    /**
     * @since 2.8.9
     */
    private final static ObjectIdInfo EMPTY = new ObjectIdInfo(PropertyName.NO_NAME, Object.class, null, false, null);

    public ObjectIdInfo(PropertyName name, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen,
            Class<? extends ObjectIdResolver> resolver)
    {
        this(name, scope, gen, false, resolver);
    }

    protected ObjectIdInfo(PropertyName prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen,
            boolean alwaysAsId)
    {
        this(prop, scope, gen, alwaysAsId, SimpleObjectIdResolver.class);

    }

    protected ObjectIdInfo(PropertyName prop, Class<?> scope, Class<? extends ObjectIdGenerator<?>> gen,
            boolean alwaysAsId, Class<? extends ObjectIdResolver> resolver)
    {
        _propertyName = prop;
        _scope = scope;
        _generator = gen;
        _alwaysAsId = alwaysAsId;
        if (resolver == null) {
            resolver = SimpleObjectIdResolver.class;
        }
        _resolver = resolver;
    }

    public static ObjectIdInfo empty() {
        return EMPTY;
    }

    public ObjectIdInfo withAlwaysAsId(boolean state) {
        if (_alwaysAsId == state) {
            return this;
        }
        return new ObjectIdInfo(_propertyName, _scope, _generator, state, _resolver);
    }

    public PropertyName getPropertyName() { return _propertyName; }
    public Class<?> getScope() { return _scope; }
    public Class<? extends ObjectIdGenerator<?>> getGeneratorType() { return _generator; }
    public Class<? extends ObjectIdResolver> getResolverType() { return _resolver; }
    public boolean getAlwaysAsId() { return _alwaysAsId; }

    @Override
    public String toString() {
        return "ObjectIdInfo: propName="+_propertyName
                +", scope="+ClassUtil.nameOf(_scope)
                +", generatorType="+ClassUtil.nameOf(_generator)
                +", alwaysAsId="+_alwaysAsId;
    }
}

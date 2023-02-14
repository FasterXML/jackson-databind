package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.ser.*;

public class PropertyBasedObjectIdGenerator
    extends ObjectIdGenerators.PropertyGenerator
{
    private static final long serialVersionUID = 1L;

    protected final BeanPropertyWriter _property;

    public PropertyBasedObjectIdGenerator(ObjectIdInfo oid, BeanPropertyWriter prop)
    {
        this(oid.getScope(), prop);
    }

    protected PropertyBasedObjectIdGenerator(Class<?> scope, BeanPropertyWriter prop)
    {
        super(scope);
        _property = prop;
    }

    /**
     * We must override this method, to prevent errors when scopes are the same,
     * but underlying class (on which to access property) is different.
     */
    @Override
    public boolean canUseFor(ObjectIdGenerator<?> gen) {
        if (gen.getClass() == getClass()) {
            PropertyBasedObjectIdGenerator other = (PropertyBasedObjectIdGenerator) gen;
            if (other.getScope() == _scope) {
                /* 26-Jul-2012, tatu: This is actually not enough, because the property
                 *   accessor within BeanPropertyWriter won't work for other property fields
                 *  (see [https://github.com/FasterXML/jackson-module-jaxb-annotations/issues/9]
                 *  for details).
                 *  So we need to verify that underlying property is actually the same.
                 */
                return (other._property == _property);
            }
        }
        return false;
    }

    @Override
    public Object generateId(Object forPojo) {
        try {
            return _property.get(forPojo);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Problem accessing property '"
                    +_property.getName()+"': "+e.getMessage(), e);
        }
    }

    @Override
    public ObjectIdGenerator<Object> forScope(Class<?> scope) {
        return (scope == _scope) ? this : new PropertyBasedObjectIdGenerator(scope, _property);
    }

    @Override
    public ObjectIdGenerator<Object> newForSerialization(Object context) {
        // No state, can return this
        return this;
    }

    @Override
    public com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey key(Object key) {
        if (key == null) {
            return null;
        }
        // should we use general type for all; or type of property itself?
        return new IdKey(getClass(), _scope, key);
    }

}

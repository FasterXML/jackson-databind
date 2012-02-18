package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.introspect.ObjectIdInfo;
import com.fasterxml.jackson.databind.ser.*;

public class PropertyBasedObjectIdGenerator
	extends ObjectIdGenerators.PropertyGenerator
{
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
        // should we use general type for all; or type of property itself?
        return new IdKey(getClass(), _scope, key);
    }

}

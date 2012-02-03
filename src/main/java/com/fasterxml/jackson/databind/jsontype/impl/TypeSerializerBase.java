package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public abstract class TypeSerializerBase extends TypeSerializer
{
    protected final TypeIdResolver _idResolver;

    protected final BeanProperty _property;
    
    protected TypeSerializerBase(TypeIdResolver idRes, BeanProperty property)
    {
        _idResolver = idRes;
        _property = property;
    }

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public String getPropertyName() { return null; }
    
    @Override
    public TypeIdResolver getTypeIdResolver() { return _idResolver; }

    /*
    /**********************************************************
    /* Helper methods for subclasses
    /**********************************************************
     */

    protected String idFromValue(Object value) {
        return _idResolver.idFromValue(value);
    }

    protected String idFromValueAndType(Object value, Class<?> type) {
        return _idResolver.idFromValueAndType(value, type);
    }
}

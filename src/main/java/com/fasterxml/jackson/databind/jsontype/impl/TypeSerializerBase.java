package com.fasterxml.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.SerializerProvider;
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

    /*
    /**********************************************************
    /* Base implementations, simple accessors
    /**********************************************************
     */

    @Override
    public abstract JsonTypeInfo.As getTypeInclusion();

    @Override
    public String getPropertyName() { return null; }
    
    @Override
    public TypeIdResolver getTypeIdResolver() { return _idResolver; }

    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator g, SerializerProvider ctxt,
            WritableTypeId idMetadata) throws JacksonException
    {
        _generateTypeId(ctxt, idMetadata);
        return g.writeTypePrefix(idMetadata);
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g, SerializerProvider ctxt,
            WritableTypeId idMetadata) throws JacksonException
    {
        return g.writeTypeSuffix(idMetadata);
    }

    /**
     * Helper method that will generate type id to use, if not already passed.
     */
    protected void _generateTypeId(DatabindContext ctxt, WritableTypeId idMetadata) {
        Object id = idMetadata.id;
        if (id == null) {
            final Object value = idMetadata.forValue;
            Class<?> typeForId = idMetadata.forValueType;
            if (typeForId == null) {
                id = idFromValue(ctxt, value);
            } else {
                id = idFromValueAndType(ctxt, value, typeForId);
            }
            idMetadata.id = id;
        }
    }

    /*
    /**********************************************************
    /* Helper methods for subclasses
    /**********************************************************
     */

    protected String idFromValue(DatabindContext ctxt, Object value) {
        String id = _idResolver.idFromValue(ctxt, value);
        if (id == null) {
            handleMissingId(value);
        }
        return id;
    }

    protected String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
        String id = _idResolver.idFromValueAndType(ctxt, value, type);
        if (id == null) {
            handleMissingId(value);
        }
        return id;
    }

    // As per [databind#633], maybe better just not do anything...
    protected void handleMissingId(Object value) {
        /*
        String typeDesc = ClassUtil.classNameOf(value, "NULL");
        throw new IllegalArgumentException("Cannot resolve type id for "
                +typeDesc+" (using "+_idResolver.getClass().getName()+")");
                */
    }
}

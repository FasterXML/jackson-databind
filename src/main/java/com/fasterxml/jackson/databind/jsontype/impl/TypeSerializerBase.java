package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.core.type.WritableTypeId;
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
    public WritableTypeId writeTypePrefix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
    {
        _generateTypeId(idMetadata);
        // 16-Jan-2022, tatu: As per [databind#3373], skip for null typeId.
        //    And return "null" so that matching "writeTypeSuffix" call should
        //    be avoided as well.
        if (idMetadata.id == null) {
            return null;
        }
        return g.writeTypePrefix(idMetadata);
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
    {
        // 16-Jan-2022, tatu: As per [databind#3373], skip for null:
        if (idMetadata == null) {
            return null;
        }
        return g.writeTypeSuffix(idMetadata);
    }

    /**
     * Helper method that will generate type id to use, if not already passed.
     *
     * @since 2.9
     */
    protected void _generateTypeId(WritableTypeId idMetadata) {
        Object id = idMetadata.id;
        if (id == null) {
            final Object value = idMetadata.forValue;
            Class<?> typeForId = idMetadata.forValueType;
            if (typeForId == null) {
                id = idFromValue(value);
            } else {
                id = idFromValueAndType(value, typeForId);
            }
            idMetadata.id = id;
        }
    }

    /*
    /**********************************************************
    /* Helper methods for subclasses
    /**********************************************************
     */

    protected String idFromValue(Object value) {
        String id = _idResolver.idFromValue(value);
        if (id == null) {
            handleMissingId(value);
        }
        return id;
    }

    protected String idFromValueAndType(Object value, Class<?> type) {
        String id = _idResolver.idFromValueAndType(value, type);
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

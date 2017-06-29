package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
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
    public void writeTypePrefix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
    {
        final Object value = idMetadata.forValue;
        // First: generate id if not passed
        final Object id = _idFor(idMetadata);
        // just for now...
        final String idStr = (id instanceof String) ? (String) id : String.valueOf(id);
        // Then dispatch to proper method (... for now)
        if (idMetadata.valueShape == JsonToken.START_OBJECT) {
            writeCustomTypePrefixForObject(value, g, idStr);
        } else if (idMetadata.valueShape == JsonToken.START_ARRAY) {
            writeCustomTypePrefixForArray(value, g, idStr);
        } else { // scalar
            writeCustomTypePrefixForScalar(value, g, idStr);
        }
    }

    @Override
    public void writeTypeSuffix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
    {
        final Object value = idMetadata.forValue;
        final Object id = idMetadata.id;
        // just for now...
        final String idStr = (id instanceof String) ? (String) id : String.valueOf(id);
        // Then dispatch to proper method (... for now)
        if (idMetadata.valueShape == JsonToken.START_OBJECT) {
            writeCustomTypeSuffixForObject(value, g, idStr);
        } else if (idMetadata.valueShape == JsonToken.START_ARRAY) {
            writeCustomTypeSuffixForArray(value, g, idStr);
        } else { // scalar
            writeCustomTypeSuffixForScalar(value, g, idStr);
        }
    }

    protected Object _idFor(WritableTypeId idParams) {
        Object id = idParams.id;
        if (id == null) {
            final Object value = idParams.forValue;
            Class<?> typeForId = idParams.forValueType;
            if (typeForId == null) {
                id = idFromValue(value);
            } else {
                id = idFromValueAndType(value, typeForId);
            }
            idParams.id = id;
        }
        return id;
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
        throw new IllegalArgumentException("Can not resolve type id for "
                +typeDesc+" (using "+_idResolver.getClass().getName()+")");
                */
    }
}

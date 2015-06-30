package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer that preferably embeds type information as an additional
 * JSON Object property, if possible (when resulting serialization would
 * use JSON Object). If this is not possible (for JSON Arrays, scalars),
 * uses a JSON Array wrapper (similar to how
 * {@link As#WRAPPER_ARRAY} always works) as a fallback.
 */
public class AsPropertyTypeSerializer
    extends AsArrayTypeSerializer
{
    protected final String _typePropertyName;

    public AsPropertyTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName)
    {
        super(idRes, property);
        _typePropertyName = propName;
    }

    @Override
    public AsPropertyTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsPropertyTypeSerializer(this._idResolver, prop, this._typePropertyName);
    }
    
    @Override
    public String getPropertyName() { return _typePropertyName; }

    @Override
    public As getTypeInclusion() { return As.PROPERTY; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException
    {
        final String typeId = idFromValue(value);
        if (typeId == null) {
            jgen.writeStartObject();
        } else if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
            jgen.writeStringField(_typePropertyName, typeId);
        }
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (typeId == null) {
            jgen.writeStartObject();
        } else if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
            jgen.writeStringField(_typePropertyName, typeId);
        }
    }
    
    //public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
    //public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type)
    //public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
    //public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type)

    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException {
        // always need to close, regardless of whether its native type id or not
        jgen.writeEndObject();
    }

    //public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
    //public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)


    /*
    /**********************************************************
    /* Writing with custom type id
    /**********************************************************
     */

    // Only need to override Object-variants
    
    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException
    {
        if (typeId == null) {
            jgen.writeStartObject();
        } else if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
            jgen.writeStringField(_typePropertyName, typeId);
        }
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
        jgen.writeEndObject();
    }
}

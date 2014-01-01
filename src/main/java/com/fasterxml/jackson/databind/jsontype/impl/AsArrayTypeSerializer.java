package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer that will embed type information in an array,
 * as the first element, and actual value as the second element.
 */
public class AsArrayTypeSerializer extends TypeSerializerBase
{
    public AsArrayTypeSerializer(TypeIdResolver idRes, BeanProperty property) {
        super(idRes, property);
    }

    @Override
    public AsArrayTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsArrayTypeSerializer(this._idResolver, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_ARRAY; }
    
    /*
    /**********************************************************
    /* Writing prefixes
    /**********************************************************
     */
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException {
        final String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartObject();
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartObject();
    }
    
    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen) throws IOException {
        final String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartArray();
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartArray();
    }
    
    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen) throws IOException {
        final String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            // only need the wrapper array
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            // only need the wrapper array
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
    }

    /*
    /**********************************************************
    /* Writing suffixes
    /**********************************************************
     */
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException {
        jgen.writeEndObject();
        if (!jgen.canWriteTypeId()) {
            jgen.writeEndArray();
        }
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen) throws IOException {
        // first array caller needs to close, then wrapper array
        jgen.writeEndArray();
        if (!jgen.canWriteTypeId()) {
            jgen.writeEndArray();
        }
    }

    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen) throws IOException {
        if (!jgen.canWriteTypeId()) {
            // just the wrapper array to close
            jgen.writeEndArray();
        }
    }
    
    /*
    /**********************************************************
    /* Writing with custom type id
    /**********************************************************
     */

    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartObject();
    }
    
    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
        jgen.writeStartArray();
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        } else {
            jgen.writeStartArray();
            jgen.writeString(typeId);
        }
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (!jgen.canWriteTypeId()) {
            writeTypeSuffixForObject(value, jgen); // standard impl works fine
        }
    }

    @Override
    public void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (!jgen.canWriteTypeId()) {
            writeTypeSuffixForArray(value, jgen); // standard impl works fine
        }
    }

    @Override
    public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (!jgen.canWriteTypeId()) {
            writeTypeSuffixForScalar(value, jgen); // standard impl works fine
        }
    }
}

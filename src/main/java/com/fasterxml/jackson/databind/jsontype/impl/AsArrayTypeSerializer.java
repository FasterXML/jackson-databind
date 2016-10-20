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
        return (_property == prop) ? this : new AsArrayTypeSerializer(_idResolver, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_ARRAY; }
    
    /*
    /**********************************************************
    /* Writing prefixes
    /**********************************************************
     */
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator g) throws IOException {
        final String typeId = idFromValue(value);
        // NOTE: can not always avoid writing type id, even if null
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartObject();
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator g, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        // NOTE: can not always avoid writing type id, even if null
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartObject();
    }
    
    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g) throws IOException {
        final String typeId = idFromValue(value);
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartArray();
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartArray();
    }
    
    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g) throws IOException {
        final String typeId = idFromValue(value);
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            // only need the wrapper array
            g.writeStartArray();
            g.writeString(typeId);
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g, Class<?> type) throws IOException {
        final String typeId = idFromValueAndType(value, type);
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            // only need the wrapper array
            g.writeStartArray();
            g.writeString(typeId);
        }
    }

    /*
    /**********************************************************
    /* Writing suffixes
    /**********************************************************
     */
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator g) throws IOException {
        g.writeEndObject();
        if (!g.canWriteTypeId()) {
            g.writeEndArray();
        }
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator g) throws IOException {
        // first array caller needs to close, then wrapper array
        g.writeEndArray();
        if (!g.canWriteTypeId()) {
            g.writeEndArray();
        }
    }

    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator g) throws IOException {
        if (!g.canWriteTypeId()) {
            // just the wrapper array to close
            g.writeEndArray();
        }
    }
    
    /*
    /**********************************************************
    /* Writing with custom type id
    /**********************************************************
     */

    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartObject();
    }
    
    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
        g.writeStartArray();
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        if (g.canWriteTypeId()) {
            _writeTypeId(g, typeId);
        } else {
            g.writeStartArray();
            g.writeString(typeId);
        }
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        if (!g.canWriteTypeId()) {
            writeTypeSuffixForObject(value, g); // standard impl works fine
        }
    }

    @Override
    public void writeCustomTypeSuffixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        if (!g.canWriteTypeId()) {
            writeTypeSuffixForArray(value, g); // standard impl works fine
        }
    }

    @Override
    public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        if (!g.canWriteTypeId()) {
            writeTypeSuffixForScalar(value, g); // standard impl works fine
        }
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    // @since 2.9
    protected final void _writeTypeId(JsonGenerator g, String typeId) throws IOException
    {
        if (typeId != null) {
            g.writeTypeId(typeId);
        }
    }
}

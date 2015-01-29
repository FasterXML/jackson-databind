package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type wrapper that tries to use an extra JSON Object, with a single
 * entry that has type name as key, to serialize type information.
 * If this is not possible (value is serialize as array or primitive),
 * will use {@link As#WRAPPER_ARRAY} mechanism as fallback: that is,
 * just use a wrapping array with type information as the first element
 * and value as second.
 */
public class AsWrapperTypeSerializer extends TypeSerializerBase
{
    public AsWrapperTypeSerializer(TypeIdResolver idRes, BeanProperty property) {
        super(idRes, property);
    }

    @Override
    public AsWrapperTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsWrapperTypeSerializer(this._idResolver, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_OBJECT; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException
    {
        String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartObject();
        } else {
            // wrapper
            jgen.writeStartObject();
            // and then JSON Object start caller wants

            // 28-Jan-2015, tatu: No really good answer here; can not really change
            //   structure, so change null to empty String...
            jgen.writeObjectFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartObject();
        } else {
            // wrapper
            jgen.writeStartObject();
            // and then JSON Object start caller wants

            // 28-Jan-2015, tatu: No really good answer here; can not really change
            //   structure, so change null to empty String...
            jgen.writeObjectFieldStart(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen) throws IOException
    {
        String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartArray();
        } else {
            // can still wrap ok
            jgen.writeStartObject();
            jgen.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartArray();
        } else {
            // can still wrap ok
            jgen.writeStartObject();
            // and then JSON Array start caller wants
            jgen.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen) throws IOException {
        final String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
        } else {
            // can still wrap ok
            jgen.writeStartObject();
            jgen.writeFieldName(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
        } else {
            // can still wrap ok
            jgen.writeStartObject();
            jgen.writeFieldName(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException
    {
        // first close JSON Object caller used
        jgen.writeEndObject();
        if (!jgen.canWriteTypeId()) {
            // and then wrapper
            jgen.writeEndObject();
        }
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen) throws IOException
    {
        // first close array caller needed
        jgen.writeEndArray();
        if (!jgen.canWriteTypeId()) {
            // then wrapper object
            jgen.writeEndObject();
        }
    }
    
    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen) throws IOException {
        if (!jgen.canWriteTypeId()) {
            // just need to close the wrapper object
            jgen.writeEndObject();
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
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
            jgen.writeObjectFieldStart(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
            jgen.writeStartArray();
        } else {
            jgen.writeStartObject();
            jgen.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId) throws IOException {
        if (jgen.canWriteTypeId()) {
            if (typeId != null) {
                jgen.writeTypeId(typeId);
            }
        } else {
            jgen.writeStartObject();
            jgen.writeFieldName(_validTypeId(typeId));
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

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */
    
    /**
     * @since 2.6.0
     */
    protected String _validTypeId(String typeId) {
        return (typeId == null) ? "" : typeId;
    }
}

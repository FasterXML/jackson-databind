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
    public void writeTypePrefixForObject(Object value, JsonGenerator g) throws IOException
    {
        String typeId = idFromValue(value);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartObject();
        } else {
            // wrapper
            g.writeStartObject();
            // and then JSON Object start caller wants

            // 28-Jan-2015, tatu: No really good answer here; can not really change
            //   structure, so change null to empty String...
            g.writeObjectFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator g, Class<?> type) throws IOException
    {
        String typeId = idFromValueAndType(value, type);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartObject();
        } else {
            // wrapper
            g.writeStartObject();
            // and then JSON Object start caller wants

            // 28-Jan-2015, tatu: No really good answer here; can not really change
            //   structure, so change null to empty String...
            g.writeObjectFieldStart(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g) throws IOException
    {
        String typeId = idFromValue(value);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartArray();
        } else {
            // can still wrap ok
            g.writeStartObject();
            g.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator g, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartArray();
        } else {
            // can still wrap ok
            g.writeStartObject();
            // and then JSON Array start caller wants
            g.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g) throws IOException {
        final String typeId = idFromValue(value);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
        } else {
            // can still wrap ok
            g.writeStartObject();
            g.writeFieldName(_validTypeId(typeId));
        }
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator g, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
        } else {
            // can still wrap ok
            g.writeStartObject();
            g.writeFieldName(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator g) throws IOException
    {
        // first close JSON Object caller used
        g.writeEndObject();
        if (!g.canWriteTypeId()) {
            // and then wrapper
            g.writeEndObject();
        }
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator g) throws IOException
    {
        // first close array caller needed
        g.writeEndArray();
        if (!g.canWriteTypeId()) {
            // then wrapper object
            g.writeEndObject();
        }
    }
    
    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator g) throws IOException {
        if (!g.canWriteTypeId()) {
            // just need to close the wrapper object
            g.writeEndObject();
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
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartObject();
        } else {
            g.writeStartObject();
            g.writeObjectFieldStart(_validTypeId(typeId));
        }
    }
    
    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
            g.writeStartArray();
        } else {
            g.writeStartObject();
            g.writeArrayFieldStart(_validTypeId(typeId));
        }
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        if (g.canWriteTypeId()) {
            if (typeId != null) {
                g.writeTypeId(typeId);
            }
        } else {
            g.writeStartObject();
            g.writeFieldName(_validTypeId(typeId));
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
    
    /**
     * Helper method used to ensure that intended type id is output as something that is valid:
     * currently only used to ensure that `null` output is converted to an empty String.
     *
     * @since 2.6
     */
    protected String _validTypeId(String typeId) {
        return (typeId == null) ? "" : typeId;
    }
}

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
 * 
 * @author tatu
 */
public class AsWrapperTypeSerializer
    extends TypeSerializerBase
{
    public AsWrapperTypeSerializer(TypeIdResolver idRes, BeanProperty property)
    {
        super(idRes, property);
    }

    @Override
    public AsWrapperTypeSerializer forProperty(BeanProperty prop) {
        if (_property == prop) return this;
        return new AsWrapperTypeSerializer(this._idResolver, prop);
    }
    
    @Override
    public As getTypeInclusion() { return As.WRAPPER_OBJECT; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // wrapper
        jgen.writeStartObject();
        // and then JSON Object start caller wants
        jgen.writeObjectFieldStart(idFromValue(value));
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen,
            Class<?> type)
        throws IOException, JsonProcessingException
    {
        // wrapper
        jgen.writeStartObject();
        // and then JSON Object start caller wants
        jgen.writeObjectFieldStart(idFromValueAndType(value, type));
    }
    
    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // can still wrap ok
        jgen.writeStartObject();
        // and then JSON Array start caller wants
        jgen.writeArrayFieldStart(idFromValue(value));
    }

    @Override
    public void writeTypePrefixForArray(Object value, JsonGenerator jgen,
            Class<?> type)
        throws IOException, JsonProcessingException
    {
        // can still wrap ok
        jgen.writeStartObject();
        // and then JSON Array start caller wants
        jgen.writeArrayFieldStart(idFromValueAndType(value, type));
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // can still wrap ok
        jgen.writeStartObject();
        jgen.writeFieldName(idFromValue(value));
    }

    @Override
    public void writeTypePrefixForScalar(Object value, JsonGenerator jgen,
            Class<?> type)
        throws IOException, JsonProcessingException
    {
        // can still wrap ok
        jgen.writeStartObject();
        jgen.writeFieldName(idFromValueAndType(value, type));
    }
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // first close JSON Object caller used
        jgen.writeEndObject();
        // and then wrapper
        jgen.writeEndObject();
    }

    @Override
    public void writeTypeSuffixForArray(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // first close array caller needed
        jgen.writeEndArray();
        // then wrapper object
        jgen.writeEndObject();
    }
    
    @Override
    public void writeTypeSuffixForScalar(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // just need to close the wrapper object
        jgen.writeEndObject();
    }

    /*
    /**********************************************************
    /* Writing with custom type id
    /**********************************************************
     */
    
    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeObjectFieldStart(typeId);
    }
    
    @Override
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator jgen, String typeId)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeArrayFieldStart(typeId);
    }

    @Override
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator jgen, String typeId)
        throws IOException, JsonProcessingException
    {
        jgen.writeStartObject();
        jgen.writeFieldName(typeId);
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId)
            throws IOException, JsonProcessingException {
        writeTypeSuffixForObject(value, jgen); // standard impl works fine
    }

    @Override
    public void writeCustomTypeSuffixForArray(Object value, JsonGenerator jgen, String typeId)
            throws IOException, JsonProcessingException {
        writeTypeSuffixForArray(value, jgen); // standard impl works fine
    }

    @Override
    public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator jgen, String typeId)
            throws IOException, JsonProcessingException {
        writeTypeSuffixForScalar(value, jgen); // standard impl works fine
    }
}

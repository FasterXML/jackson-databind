package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer used with {@link As#EXISTING_PROPERTY} inclusion mechanism.
 * Expects type information to be a well-defined property on all sub-classes.
 * 
 * 10/15/2014 - At time of commit, deserialization identical to deserializer
 * for {@link As#PROPERTY} inclusion mechanism
 * 
 * @author fleeman (modeled after code by tatus)
 */
public class AsExistingPropertyTypeSerializer
    extends AsArrayTypeSerializer
{
    protected final String _typePropertyName;

    public AsExistingPropertyTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName)
    {
        super(idRes, property);
        _typePropertyName = propName;
    }

    @Override
    public AsExistingPropertyTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsExistingPropertyTypeSerializer(this._idResolver, prop, this._typePropertyName);
    }
    
    @Override
    public String getPropertyName() { return _typePropertyName; }

    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException
    {
        final String typeId = idFromValue(value);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
        }
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
        }
    }
    
    @Override
    public void writeTypeSuffixForObject(Object value, JsonGenerator jgen) throws IOException {
        // always need to close, regardless of whether its native type id or not
    	jgen.writeEndObject();
    }


    /*
    /**********************************************************
    /* Writing with custom type id
    /**********************************************************
     */

    // Only need to override Object-variants
    
    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException
    {
    	if (jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
            jgen.writeStartObject();
        } else {
            jgen.writeStartObject();
        }
    }

    @Override
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException {
        jgen.writeEndObject();
    }
}

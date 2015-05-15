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
 * @author fleeman (modeled after code by tatus)
 */
public class AsExistingPropertyTypeSerializer
    extends AsPropertyTypeSerializer
{

    public AsExistingPropertyTypeSerializer(TypeIdResolver idRes, BeanProperty property, String propName)
    {
        super(idRes, property, propName);
    }

    @Override
    public AsExistingPropertyTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsExistingPropertyTypeSerializer(this._idResolver, prop, this._typePropertyName);
    }
    
    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen) throws IOException
    {
        final String typeId = idFromValue(value);
        if ((typeId != null) && jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        }
        jgen.writeStartObject();
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator jgen, Class<?> type) throws IOException
    {
        final String typeId = idFromValueAndType(value, type);
        if ((typeId != null) && jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        }
        jgen.writeStartObject();
    }
    
    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator jgen, String typeId) throws IOException
    {
        if ((typeId != null) && jgen.canWriteTypeId()) {
            jgen.writeTypeId(typeId);
        }
        jgen.writeStartObject();
    }
}

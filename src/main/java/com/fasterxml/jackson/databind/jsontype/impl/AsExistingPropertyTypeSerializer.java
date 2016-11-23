package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type serializer used with {@link As#EXISTING_PROPERTY} inclusion mechanism.
 * Expects type information to be a well-defined property on all sub-classes.
 */
public class AsExistingPropertyTypeSerializer
    extends AsPropertyTypeSerializer
{
    public AsExistingPropertyTypeSerializer(TypeIdResolver idRes,
            BeanProperty property, String propName)
    {
        super(idRes, property, propName);
    }

    @Override
    public AsExistingPropertyTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this :
            new AsExistingPropertyTypeSerializer(_idResolver, prop, _typePropertyName);
    }
    
    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }
    
    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator gen) throws IOException
    {
        if (gen.canWriteTypeId()) { // only write explicitly if native type id
            final String typeId = idFromValue(value);
            if (typeId != null) {
                gen.writeTypeId(typeId);
            }
        }
        gen.writeStartObject();
    }

    @Override
    public void writeTypePrefixForObject(Object value, JsonGenerator gen, Class<?> type) throws IOException
    {
        if (gen.canWriteTypeId()) { // only write explicitly if native type id
            final String typeId = idFromValueAndType(value, type);
            if (typeId != null) {
                gen.writeTypeId(typeId);
            }
        }
        gen.writeStartObject();
    }
    
    @Override
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator gen, String typeId) throws IOException
    {
        if ((typeId != null) && gen.canWriteTypeId()) {
            gen.writeTypeId(typeId);
        }
        gen.writeStartObject();
    }
}

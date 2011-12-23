package com.fasterxml.jackson.databind.jsontype.impl;

import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.type.JavaType;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;

/**
 * Type deserializer used with {@link As#EXTERNAL_PROPERTY} inclusion mechanism.
 * Actual implementation may look bit strange since it depends on comprehensive
 * pre-processing done by {@link com.fasterxml.jackson.databind.deser.BeanDeserializer}
 * to basically transform external type id into structure that looks more like
 * "wrapper-array" style inclusion. This intermediate form is chosen to allow
 * supporting all possible JSON structures.
 * 
 * @since 1.9
 */
public class AsExternalTypeDeserializer extends AsArrayTypeDeserializer
{
    protected final String _typePropertyName;
    
    public AsExternalTypeDeserializer(JavaType bt, TypeIdResolver idRes, BeanProperty property,
            Class<?> defaultImpl,
            String typePropName)
    {
        super(bt, idRes, property, defaultImpl);
        _typePropertyName = typePropName;
    }

    @Override
    public As getTypeInclusion() {
        return As.EXTERNAL_PROPERTY;
    }

    @Override
    public String getPropertyName() { return _typePropertyName; }
}

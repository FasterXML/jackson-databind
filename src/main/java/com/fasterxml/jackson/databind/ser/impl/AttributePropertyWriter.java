package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * {@link VirtualBeanPropertyWriter} implementation used for
 * {@link com.fasterxml.jackson.databind.annotation.JsonAppend},
 * to serialize properties backed-by dynamically assignable attribute
 * values.
 * 
 * @since 2.5
 */
public class AttributePropertyWriter
    extends VirtualBeanPropertyWriter
{
    protected final String _attrName;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
/*
    protected VirtualBeanPropertyWriter(BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            JsonSerializer<?> ser, TypeSerializer typeSer, JavaType serType,
            boolean suppressNulls, Object suppressableValue)
 */
    
    public AttributePropertyWriter(String attrName,
            BeanPropertyDefinition propDef,
            Annotations contextAnnotations, JavaType declaredType,
            JsonInclude.Include inclusion)
    {
        super(propDef, contextAnnotations, declaredType,
                /* value serializer */ null, /* type serializer */ null, /* ser type */ null,
                _suppressNulls(inclusion), null);
        _attrName = attrName;
    }

    protected AttributePropertyWriter(AttributePropertyWriter base) {
        super(base);
        _attrName = base._attrName;
    }

    @Override
    public BeanPropertyDefinition withName(PropertyName newName) {
        // !!! TODO
        return null;
    }

    @Override
    public BeanPropertyDefinition withSimpleName(String newSimpleName) {
        // !!! TODO
        return null;
    }

    protected static boolean _suppressNulls(JsonInclude.Include inclusion) {
        return (inclusion != JsonInclude.Include.ALWAYS);
    }

    /*
    /**********************************************************
    /* Overrides for actual serialization, value access
    /**********************************************************
     */
    
    @Override
    protected Object value(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        return prov.getAttribute(_attrName);
    }
}

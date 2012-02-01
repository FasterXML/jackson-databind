package com.fasterxml.jackson.databind.ser;

import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;

/**
 * Class similar to {@link BeanPropertyWriter}, but that will be used
 * for serializing {@link com.fasterxml.jackson.annotation.JsonAnyGetter} annotated
 * (Map) properties
 */
public class AnyGetterWriter
{
    protected final BeanProperty _property;

    /**
     * Method (or field) that represents the "any getter"
     */
    protected final AnnotatedMember _accessor;
    
    protected MapSerializer _serializer;
    
    public AnyGetterWriter(BeanProperty property,
            AnnotatedMember accessor, MapSerializer serializer)
    {
        _accessor = accessor;
        _property = property;
        _serializer = serializer;
    }

    public void getAndSerialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws Exception
    {
        Object value = _accessor.getValue(bean);
        if (value == null) {
            return;
        }
        if (!(value instanceof Map<?,?>)) {
            throw new JsonMappingException("Value returned by 'any-getter' ("
                    +_accessor.getName()+"()) not java.util.Map but "+value.getClass().getName());
        }
        _serializer.serializeFields((Map<?,?>) value, jgen, provider);
    }

    // Note: NOT part of ResolvableSerializer...
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        _serializer = (MapSerializer) _serializer.createContextual(provider, _property);
    }
}

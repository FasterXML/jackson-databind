package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

/**
 * Object that knows how to serialize Object Ids.
 */
public class ObjectIdHandler
{
    /**
     * Logical property that represents the id.
     */
    protected final BeanProperty _property;
    
    /**
     * Serializer used for serializing id values.
     */
    protected final JsonSerializer<Object> _serializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    protected ObjectIdHandler(BeanProperty prop, JsonSerializer<?> ser)
    {
        _property = prop;
        _serializer = (JsonSerializer<Object>) ser;
    }

    public static ObjectIdHandler construct(BeanProperty prop)
    {
        return new ObjectIdHandler(prop, null);
    }

    public ObjectIdHandler createContextual(SerializerProvider provider)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = provider.findValueSerializer(_property.getType(), _property);
        return new ObjectIdHandler(_property, ser);
    }
    
    /*
    /**********************************************************
    /* Serialization API
    /**********************************************************
     */

    public boolean handleReference(Object pojo, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
    {
        Object id = provider.findObjectId(pojo, _property.getMember());
System.err.println("Handle ref for: "+pojo+", id = "+id);        
        if (id == null) {            
            return false;
        }
        _serializer.serialize(id, jgen, provider);
        return true;
    }        
}

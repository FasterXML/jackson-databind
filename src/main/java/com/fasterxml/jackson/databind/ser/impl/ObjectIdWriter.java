package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;

/**
 * Object that knows how to serialize Object Ids.
 */
public class ObjectIdWriter
{
    protected final JavaType _idType;

    protected final String _propertyName;
    
    protected final ObjectIdGenerator<?> _generator;

    /**
     * Logical property that represents the id.
     */
//    protected final BeanProperty _property;
    
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
    protected ObjectIdWriter(JavaType idType, String propName, ObjectIdGenerator<?> gen,
            JsonSerializer<?> ser)
    {
        _idType = idType;
        _propertyName = propName;
        _generator = gen;
        _serializer = (JsonSerializer<Object>) ser;
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     */
    public static ObjectIdWriter construct(JavaType idType, String propertyName,
            ObjectIdGenerator<?> generator)
    {
        return new ObjectIdWriter(idType, propertyName, generator, null);
    }

    public ObjectIdWriter withSerializer(SerializerProvider provider)
        throws JsonMappingException
    {
        JsonSerializer<?> ser = provider.findValueSerializer(_idType, null);
        return new ObjectIdWriter(_idType, _propertyName, _generator, ser);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    public JavaType getType() { return _idType; }
    public String getPropertyName() { return _propertyName; }
    
    /*
    /**********************************************************
    /* Serialization API
    /**********************************************************
     */

    /**
     * Method called to see if we could possibly just write a reference to previously
     * serialized POJO.
     */
    public boolean handleReference(Object pojo, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
    {
        Object id = provider.findObjectId(pojo);
        // if it has been serialized, just write reference:
        if (id == null) {
            return false;
        }
        _serializer.serialize(id, jgen, provider);
        return true;
    }        

    /**
     * Method called to write Object Id as regular property, in case where POJO
     * has not yet been serialized.
     */
    public void writeAsProperty(Object pojo, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
    {
        Object id = _generator.generateId(pojo);
        provider.addObjectId(pojo, id);
        // if it has been serialized, just write reference:
        jgen.writeFieldName(_propertyName);
        _serializer.serialize(id, jgen, provider);
    }        
}

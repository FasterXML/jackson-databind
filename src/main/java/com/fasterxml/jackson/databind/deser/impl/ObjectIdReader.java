package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.databind.*;

/**
 * Object that knows how to serialize Object Ids.
 */
public final class ObjectIdReader
{
    public final JavaType idType;

    public final String propertyName;
    
    /**
     * Blueprint generator instance: actual instance will be
     * fetched from {@link SerializerProvider} using this as
     * the key.
     */
    public final ObjectIdGenerator<?> generator;
    
    /**
     * Serializer used for serializing id values.
     */
    public final JsonDeserializer<Object> deserializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    protected ObjectIdReader(JavaType t, String propName, ObjectIdGenerator<?> gen,
            JsonDeserializer<?> deser)
    {
        idType = t;
        propertyName = propName;
        generator = gen;
        deserializer = (JsonDeserializer<Object>) deser;
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     */
    public static ObjectIdReader construct(JavaType idType, String propName,
            ObjectIdGenerator<?> generator)
    {
        return new ObjectIdReader(idType, propName, generator, null);
    }

    public ObjectIdReader withSerializer(JsonDeserializer<?> ser) {
        return new ObjectIdReader(idType, propertyName, generator, ser);
    }
}

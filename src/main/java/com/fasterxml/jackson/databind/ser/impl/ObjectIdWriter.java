package com.fasterxml.jackson.databind.ser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;

/**
 * Object that knows how to serialize Object Ids.
 */
public final class ObjectIdWriter
{
    public final JavaType idType;

    /**
     * Name of id property to write, if not null: if null, should
     * only write references, but id property is handled by some
     * other entity.
     */
    public final SerializedString propertyName;
    
    /**
     * Blueprint generator instance: actual instance will be
     * fetched from {@link SerializerProvider} using this as
     * the key.
     */
    public final ObjectIdGenerator<?> generator;
    
    /**
     * Serializer used for serializing id values.
     */
    public final JsonSerializer<Object> serializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    protected ObjectIdWriter(JavaType t, SerializedString propName, ObjectIdGenerator<?> gen,
            JsonSerializer<?> ser)
    {
        idType = t;
        propertyName = propName;
        generator = gen;
        serializer = (JsonSerializer<Object>) ser;
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     */
    public static ObjectIdWriter construct(JavaType idType, String propName,
            ObjectIdGenerator<?> generator)
    {
        SerializedString serName = (propName == null) ? null : new SerializedString(propName);
        return new ObjectIdWriter(idType, serName, generator, null);
    }

    public ObjectIdWriter withSerializer(JsonSerializer<?> ser) {
        return new ObjectIdWriter(idType, propertyName, generator, ser);
    }
}

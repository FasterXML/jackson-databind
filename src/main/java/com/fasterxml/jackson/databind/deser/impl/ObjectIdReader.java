package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;

/**
 * Object that knows how to serialize Object Ids.
 */
public final class ObjectIdReader
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public final JavaType idType;

    public final PropertyName propertyName;
    
    /**
     * Blueprint generator instance: actual instance will be
     * fetched from {@link SerializerProvider} using this as
     * the key.
     */
    public final ObjectIdGenerator<?> generator;
    
    /**
     * Deserializer used for deserializing id values.
     */
    public final JsonDeserializer<Object> deserializer;

    public final SettableBeanProperty idProperty;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    protected ObjectIdReader(JavaType t, PropertyName propName, ObjectIdGenerator<?> gen,
            JsonDeserializer<?> deser, SettableBeanProperty idProp)
    {
        idType = t;
        propertyName = propName;
        generator = gen;
        deserializer = (JsonDeserializer<Object>) deser;
        idProperty = idProp;
    }

    @Deprecated // since 2.3
    protected ObjectIdReader(JavaType t, String propName, ObjectIdGenerator<?> gen,
            JsonDeserializer<?> deser, SettableBeanProperty idProp)
    {
        this(t, new PropertyName(propName), gen, deser, idProp);
    }

    /**
     * Factory method called by {@link com.fasterxml.jackson.databind.ser.std.BeanSerializerBase}
     * with the initial information based on standard settings for the type
     * for which serializer is being built.
     */
    public static ObjectIdReader construct(JavaType idType, PropertyName propName,
            ObjectIdGenerator<?> generator, JsonDeserializer<?> deser,
            SettableBeanProperty idProp)
    {
        return new ObjectIdReader(idType, propName, generator, deser, idProp);
    }
    
    @Deprecated // since 2.3
    public static ObjectIdReader construct(JavaType idType, String propName,
            ObjectIdGenerator<?> generator, JsonDeserializer<?> deser,
            SettableBeanProperty idProp)
    {
        return construct(idType, new PropertyName(propName), generator, deser, idProp);
    }
}

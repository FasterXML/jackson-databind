package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.type.*;

/**
 * Interface that defines API for simple extensions that can provide additional serializers
 * for various types. Access is by a single callback method; instance is to either return
 * a configured {@link JsonSerializer} for specified type, or null to indicate that it
 * does not support handling of the type. In latter case, further calls can be made
 * for other providers; in former case returned serializer is used for handling of
 * instances of specified type.
 */
public interface Serializers
{
    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified type, which is not of a container or reference type (for which
     * other methods are called).
     *
     * @param type Fully resolved type of instances to serialize
     * @param config Serialization configuration in use
     * @param beanDesc Additional information about type
     *
     * @return Configured serializer to use for the type; or null if implementation
     *    does not recognize or support type
     */
    public JsonSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * given {@link ReferenceType}
     *
     * @since 2.7
     */
    public JsonSerializer<?> findReferenceSerializer(SerializationConfig config,
            ReferenceType type, BeanDescription beanDesc,
            TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified array type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     */
    public JsonSerializer<?> findArraySerializer(SerializationConfig config,
            ArrayType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified {@link java.util.Collection} type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     */
    public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified "Collection-like" type (type that acts like {@link java.util.Collection},
     * but does not implement it).
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     */
    public JsonSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
            CollectionLikeType type, BeanDescription beanDesc,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified {@link java.util.Map} type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     */
    public JsonSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer);

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified "Map-like" type (type that acts like {@link java.util.Map},
     * but does not implement it).
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     */
    public JsonSerializer<?> findMapLikeSerializer(SerializationConfig config,
            MapLikeType type, BeanDescription beanDesc,
            JsonSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer);

    /**
     * Basic {@link Serializers} implementation that implements all methods but provides
     * no serializers. Its main purpose is to serve as a base class so that
     * sub-classes only need to override methods they need.
     */
    public static class Base implements Serializers
    {
        @Override
        public JsonSerializer<?> findSerializer(SerializationConfig config,
                JavaType type, BeanDescription beanDesc)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> findReferenceSerializer(SerializationConfig config,
                ReferenceType type, BeanDescription beanDesc,
                TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer) {
            // 21-Oct-2015, tatu: For backwards compatibility, let's delegate to "bean" variant,
            //    for 2.7 -- remove work-around from 2.8 or later
            return findSerializer(config, type, beanDesc);
        }

        @Override
        public JsonSerializer<?> findArraySerializer(SerializationConfig config,
                ArrayType type, BeanDescription beanDesc,
                TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> findCollectionSerializer(SerializationConfig config,
                CollectionType type, BeanDescription beanDesc,
                TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
                CollectionLikeType type, BeanDescription beanDesc,
                TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> findMapSerializer(SerializationConfig config,
                MapType type, BeanDescription beanDesc,
                JsonSerializer<Object> keySerializer,
                TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public JsonSerializer<?> findMapLikeSerializer(SerializationConfig config,
                MapLikeType type, BeanDescription beanDesc,
                JsonSerializer<Object> keySerializer,
                TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
        {
            return null;
        }
    }
}

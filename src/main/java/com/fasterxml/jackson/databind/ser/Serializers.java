package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.type.*;

/**
 * Interface that defines API for simple extensions that can provide additional serializers
 * for various types. Access is by a single callback method; instance is to either return
 * a configured {@link ValueSerializer} for specified type, or null to indicate that it
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
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     *    
     * @return Configured serializer to use for the type; or null if implementation
     *    does not recognize or support type
     */
    default ValueSerializer<?> findSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides)
    {
        return null;
    }

    /**
     * Method called by serialization framework first time a serializer is needed for
     * given {@link ReferenceType}
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findReferenceSerializer(SerializationConfig config,
            ReferenceType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer contentTypeSerializer, ValueSerializer<Object> contentValueSerializer)
    {
        return null;
    }
    
    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified array type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findArraySerializer(SerializationConfig config, ArrayType type,
            BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        return null;
    }

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified {@link java.util.Collection} type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findCollectionSerializer(SerializationConfig config,
            CollectionType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        return null;
    }

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified "Collection-like" type (type that acts like {@link java.util.Collection},
     * but does not implement it).
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
            CollectionLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        return null;
    }
    
    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified {@link java.util.Map} type.
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findMapSerializer(SerializationConfig config,
            MapType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            ValueSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        return null;
    }

    /**
     * Method called by serialization framework first time a serializer is needed for
     * specified "Map-like" type (type that acts like {@link java.util.Map},
     * but does not implement it).
     * Implementation should return a serializer instance if it supports
     * specified type; or null if it does not.
     *
     * @param formatOverrides (nullable) Optional format overrides (usually from property definition),
     *     to change definitions that {@code beanDesc} may have (and which are NOT included). Usually
     *     combined calling {@code Serializers.Base#calculateEffectiveFormat}.
     */
    default ValueSerializer<?> findMapLikeSerializer(SerializationConfig config,
            MapLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
            ValueSerializer<Object> keySerializer,
            TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
    {
        return null;
    }

    /**
     * Method called in case that a given type or property is declared to use shape
     * {@code JsonFormat.Shape.POJO} and is expected to be serialized "as POJO", that is,
     * as an (JSON) Object. This is usually NOT handled by extension modules as core
     * databind knows how to do this, but sometimes it may be necessary to override
     * this behavior.
     *
     * @since 3.0
     */
    default ValueSerializer<?> findExplicitPOJOSerializer(SerializationConfig config,
            JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides)
    {
        return null;
    }

    /**
     * Basic {@link Serializers} implementation that implements all methods but provides
     * no serializers. Its main purpose is to serve as a base class so that
     * sub-classes only need to override methods they need.
     */
    public static class Base implements Serializers
    {
        @Override
        public ValueSerializer<?> findSerializer(SerializationConfig config,
                JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides)
        {
            return null;
        }

        @Override
        public ValueSerializer<?> findReferenceSerializer(SerializationConfig config,
                ReferenceType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                TypeSerializer contentTypeSerializer, ValueSerializer<Object> contentValueSerializer) {
            return null;
        }

        @Override
        public ValueSerializer<?> findArraySerializer(SerializationConfig config,
                ArrayType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public ValueSerializer<?> findCollectionSerializer(SerializationConfig config,
                CollectionType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public ValueSerializer<?> findCollectionLikeSerializer(SerializationConfig config,
                CollectionLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
        {
            return null;
        }
            
        @Override
        public ValueSerializer<?> findMapSerializer(SerializationConfig config,
                MapType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                ValueSerializer<Object> keySerializer,
                TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public ValueSerializer<?> findMapLikeSerializer(SerializationConfig config,
                MapLikeType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides,
                ValueSerializer<Object> keySerializer,
                TypeSerializer elementTypeSerializer, ValueSerializer<Object> elementValueSerializer)
        {
            return null;
        }

        @Override
        public ValueSerializer<?> findExplicitPOJOSerializer(SerializationConfig config,
                JavaType type, BeanDescription beanDesc, JsonFormat.Value formatOverrides)
        {
            return null;
        }
        
        /*
        /******************************************************************
        /* Helper methods
        /******************************************************************
         */

        /**
         * Helper method for determining effective combination of formatting settings
         * from combination of Class annotations and config overrides for type and
         * possible per-property overrides (in this order of precedence from lowest
         * to highest).
         */
        protected JsonFormat.Value calculateEffectiveFormat(BeanDescription beanDesc,
                Class<?> baseType, JsonFormat.Value formatOverrides)
        {
            JsonFormat.Value fromType = beanDesc.findExpectedFormat(baseType);
            if (formatOverrides == null) {
                return fromType;
            }
            return JsonFormat.Value.merge(fromType, formatOverrides);
        }
    }
}

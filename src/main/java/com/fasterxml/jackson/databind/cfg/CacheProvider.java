package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LookupCache;
import com.fasterxml.jackson.databind.util.TypeKey;

/**
 * Interface that defines API Jackson uses for constructing various internal
 * caches. This allows configuring custom caches and cache configurations.
 * A {@link CacheProvider} instance will be configured through a builder such as
 * {@link com.fasterxml.jackson.databind.json.JsonMapper.Builder#cacheProvider(CacheProvider)}
 *
 * @since 2.16
 */
public interface CacheProvider
    extends java.io.Serializable
{
    /**
     * Method for constructing a {@link LookupCache} instance to be used by
     * {@link DeserializerCache}.
     *
     * @return {@link LookupCache} instance for use by {@link DeserializerCache}.
     */
    LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config);

    /**
     * Method for constructing a {@link LookupCache} instance to be used by
     * {@link com.fasterxml.jackson.databind.ser.SerializerCache}.
     *
     * @return {@link LookupCache} instance for constructing {@link com.fasterxml.jackson.databind.ser.SerializerCache}.
     */
    LookupCache<TypeKey, JsonSerializer<Object>> forSerializerCache(SerializationConfig config);

    /**
     * Method for constructing a {@link LookupCache} instance to be used by
     * {@link com.fasterxml.jackson.databind.type.TypeFactory}.
     *
     * @return {@link LookupCache} instance for constructing {@link com.fasterxml.jackson.databind.type.TypeFactory}.
     */
    LookupCache<Object, JavaType> forTypeFactory();
}

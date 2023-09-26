package tools.jackson.databind.cfg;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializerCache;
import tools.jackson.databind.util.LookupCache;
import tools.jackson.databind.util.TypeKey;

/**
 * Interface that defines API Jackson uses for constructing various internal
 * caches. This allows configuring custom caches and cache configurations.
 * A {@link CacheProvider} instance will be configured through a builder such as
 * {@link tools.jackson.databind.json.JsonMapper.Builder#cacheProvider(CacheProvider)}
 */
public interface CacheProvider
    extends java.io.Serializable
{
    /**
     * Method to provide a {@link LookupCache} instance for constructing {@link DeserializerCache}.
     *
     * @return {@link LookupCache} instance for constructing {@link DeserializerCache}.
     */
    LookupCache<JavaType, ValueDeserializer<Object>> forDeserializerCache(DeserializationConfig config);

    /**
     * Method to provide a {@link LookupCache} instance for constructing {@link tools.jackson.databind.ser.SerializerCache}.
     *
     * @return {@link LookupCache} instance for constructing {@link tools.jackson.databind.ser.SerializerCache}.
     */
    LookupCache<TypeKey, ValueSerializer<Object>> forSerializerCache(SerializationConfig config);

    /**
     * Method to provide a {@link LookupCache} instance for constructing {@link tools.jackson.databind.type.TypeFactory}.
     *
     * @return {@link LookupCache} instance for constructing {@link tools.jackson.databind.type.TypeFactory}.
     */
    LookupCache<Object, JavaType> forTypeFactory();
}

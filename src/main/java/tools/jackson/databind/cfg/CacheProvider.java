package tools.jackson.databind.cfg;

import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.DeserializerCache;
import tools.jackson.databind.util.LookupCache;

/**
 * Interface that defines API Jackson uses for constructing various internal
 * caches. This allows configuring custom caches and cache configurations.
 * A {@link CacheProvider} instance will be configured through a builder such as
 * {@link tools.jackson.databind.json.JsonMapper.Builder#cacheProvider(CacheProvider)}
 *
 * @since 2.16
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
}

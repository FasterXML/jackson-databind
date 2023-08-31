package tools.jackson.databind.cfg;

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
    // !!! TODO: add methods
}

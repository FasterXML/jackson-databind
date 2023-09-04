package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * The default implementation of {@link CacheProvider}.
 * Configuration is builder-based via {@link DefaultCacheProvider.Builder}.
 * <p>
 * Users can either use this class or create their own {@link CacheProvider} implementation.
 *
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider
{
    private static final long serialVersionUID = 1L;

    private final static DefaultCacheProvider DEFAULT
        = new DefaultCacheProvider(DeserializerCache.DEFAULT_MAX_CACHE_SIZE);

    /**
     * Maximum size of the {@link LookupCache} instance constructed by {@link #forDeserializerCache(DeserializationConfig)}.
     *
     * @see Builder#maxDeserializerCacheSize(int)
     */
    protected final int _maxDeserializerCacheSize;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected DefaultCacheProvider(int deserializerCache)
    {
        _maxDeserializerCacheSize = deserializerCache;
    }

    /*
    /**********************************************************************
    /* Defaults
    /**********************************************************************
     */

    /**
     * @return Default {@link DefaultCacheProvider} instance using default configuration values.
     */
    public static CacheProvider defaultInstance() {
        return DEFAULT;
    }

    /*
    /**********************************************************
    /* API implementation
    /**********************************************************
     */

    /**
     * Method to provide a {@link LookupCache} instance for constructing {@link DeserializerCache}.
     * Implementation should match {@link DeserializerCache#DeserializerCache(int)}.
     *
     * @return {@link LookupCache} instance for constructing {@link DeserializerCache}.
     */
    @Override
    public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
        return _buildCache(_maxDeserializerCacheSize);
    }

    /*
    /**********************************************************
    /* Overridable factory methods
    /**********************************************************
     */

    protected <K,V> LookupCache<K,V> _buildCache(int maxSize)
    {
        // Use 1/4 of maximum size (but at most 64) for initial size
        final int initialSize = Math.min(64, maxSize >> 2);
        return new LRUMap<>(initialSize, maxSize);
    }

    /*
    /**********************************************************
    /* Builder Config
    /**********************************************************
     */

    /**
     * @return {@link Builder} instance for configuration.
     */
    public static DefaultCacheProvider.Builder builder() {
        return new Builder();
    }

    /**
     * Builder offering fluent factory methods to configure {@link DefaultCacheProvider}, keeping it immutable.
     */
    public static class Builder {

        /**
         * Maximum Size of the {@link LookupCache} instance created by {@link #forDeserializerCache(DeserializationConfig)}.
         * Corresponds to {@link DefaultCacheProvider#_maxDeserializerCacheSize}.
         */
        private int _maxDeserializerCacheSize;

        Builder() { }

        /**
         * Define the maximum size of the {@link LookupCache} instance constructed by {@link #forDeserializerCache(DeserializationConfig)}.
         * The cache is instantiated as:
         * <pre>
         *     return new LRUMap<>(Math.min(64, maxSize >> 2), maxSize);
         * </pre>
         *
         * @param maxDeserializerCacheSize Size for the {@link LookupCache} to use within {@link DeserializerCache}
         * @return this builder
         * @throws IllegalArgumentException if {@code maxDeserializerCacheSize} is negative
         * @since 2.16
         */
        public Builder maxDeserializerCacheSize(int maxDeserializerCacheSize) {
            if (maxDeserializerCacheSize < 0) {
                throw new IllegalArgumentException("Cannot set maxDeserializerCacheSize to a negative value");
            }
            _maxDeserializerCacheSize = maxDeserializerCacheSize;
            return this;
        }

        /**
         * Constructs a {@link DefaultCacheProvider} with the provided configuration values, using defaults where not specified.
         *
         * @return A {@link DefaultCacheProvider} instance with the specified configuration
         */
        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_maxDeserializerCacheSize);
        }
    }
}

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.ser.SerializerCache;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;
import com.fasterxml.jackson.databind.util.TypeKey;

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
        = new DefaultCacheProvider(DeserializerCache.DEFAULT_MAX_CACHE_SIZE, SerializerCache.DEFAULT_MAX_CACHE_SIZE, TypeFactory.DEFAULT_MAX_CACHE_SIZE);

    /**
     * Maximum size of the {@link LookupCache} instance constructed by {@link #forDeserializerCache(DeserializationConfig)}.
     *
     * @see Builder#maxDeserializerCacheSize(int)
     */
    protected final int _maxDeserializerCacheSize;

    /**
     * Maximum size of the {@link LookupCache} instance constructed by {@link #forSerializerCache(SerializationConfig)}
     *
     * @see Builder#maxSerializerCacheSize(int)
     */
    protected final int _maxSerializerCacheSize;

    /**
     * Maximum size of the {@link LookupCache} instance constructed by {@link #forTypeFactory()}.
     *
     * @see Builder#maxTypeFactoryCacheSize(int)
     */
    protected final int _maxTypeFactoryCacheSize;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected DefaultCacheProvider(int maxDeserializerCacheSize, int maxSerializerCacheSize, int maxTypeFactoryCacheSize)
    {
        _maxDeserializerCacheSize = maxDeserializerCacheSize;
        _maxSerializerCacheSize = maxSerializerCacheSize;
        _maxTypeFactoryCacheSize = maxTypeFactoryCacheSize;
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

    @Override
    public LookupCache<TypeKey, JsonSerializer<Object>> forSerializerCache(SerializationConfig config) {
        return _buildCache(_maxSerializerCacheSize);
    }

    @Override
    public LookupCache<Object, JavaType> forTypeFactory() {
        return _buildCache(_maxTypeFactoryCacheSize);
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

        /**
         * Maximum Size of the {@link LookupCache} instance created by {@link #forSerializerCache(SerializationConfig)}
         * Corresponds to {@link DefaultCacheProvider#_maxSerializerCacheSize}.
         */
        private int _maxSerializerCacheSize;

        /**
         * Maximum Size of the {@link LookupCache} instance created by {@link #forTypeFactory()}.
         * Corresponds to {@link DefaultCacheProvider#_maxTypeFactoryCacheSize}.
         */
        private int _maxTypeFactoryCacheSize;

        Builder() { }

        /**
         * Define the maximum size of the {@link LookupCache} instance constructed by {@link #forDeserializerCache(DeserializationConfig)}
         * and {@link #_buildCache(int)}.
         * <p>
         * Note that specifying a maximum size of zero prevents values from being retained in the cache.
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
         * Define the maximum size of the {@link LookupCache} instance constructed by {@link #forSerializerCache(SerializationConfig)}
         * and {@link #_buildCache(int)}
         * <p>
         * Note that specifying a maximum size of zero prevents values from being retained in the cache.
         *
         * @param maxSerializerCacheSize Size for the {@link LookupCache} to use within {@link SerializerCache}
         * @return this builder
         * @throws IllegalArgumentException if {@code maxSerializerCacheSize} is negative
         * @since 2.16
         */
        public Builder maxSerializerCacheSize(int maxSerializerCacheSize) {
            if (maxSerializerCacheSize < 0) {
                throw new IllegalArgumentException("Cannot set maxSerializerCacheSize to a negative value");
            }
            _maxSerializerCacheSize = maxSerializerCacheSize;
            return this;
        }

        /**
         * Define the maximum size of the {@link LookupCache} instance constructed by {@link #forTypeFactory()}
         * and {@link #_buildCache(int)}
         * <p>
         * Note that specifying a maximum size of zero prevents values from being retained in the cache.
         *
         * @param maxTypeFactoryCacheSize Size for the {@link LookupCache} to use within {@link com.fasterxml.jackson.databind.type.TypeFactory}
         * @return this builder
         * @throws IllegalArgumentException if {@code maxTypeFactoryCacheSize} is negative
         */
        public Builder maxTypeFactoryCacheSize(int maxTypeFactoryCacheSize) {
            if (maxTypeFactoryCacheSize < 0) {
                throw new IllegalArgumentException("Cannot set maxTypeFactoryCacheSize to a negative value");
            }
            _maxTypeFactoryCacheSize = maxTypeFactoryCacheSize;
            return this;
        }

        /**
         * Constructs a {@link DefaultCacheProvider} with the provided configuration values, using defaults where not specified.
         *
         * @return A {@link DefaultCacheProvider} instance with the specified configuration
         */
        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_maxDeserializerCacheSize, _maxSerializerCacheSize, _maxTypeFactoryCacheSize);
        }
    }
}

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;

import java.util.Objects;

/**
 * Default implementation of {@link CacheProvider}.
 * Provides builder-based custom cache configuration using {@link DefaultCacheProvider.Builder}.
 * Users can either use this class or implement their own {@link CacheProvider} imlementation.
 * <p>
 * WARNING: Configured cache instances are <br>shared</>.
 * Meaning that if you use same {@link DefaultCacheProvider} instance to construct multiple
 * {@link com.fasterxml.jackson.databind.ObjectMapper} instances, they will share the same cache instances.
 * 
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider
{
    private static final long serialVersionUID = 1L;

    /**
     * Size of {@link LookupCache} instance to create when {@link #forDeserializerCache(DeserializationConfig)}
     * is invoked.
     */
    protected final int _deserializerCacheSize;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected DefaultCacheProvider(int deserializerCache)
    {
        _deserializerCacheSize = deserializerCache;
    }
    
    /*
    /**********************************************************************
    /* Defaults
    /**********************************************************************
     */

    /**
     * @return Default {@link DefaultCacheProvider} instance with default configuration values.
     */
    public static CacheProvider defaultInstance() {
        return new DefaultCacheProvider(DeserializerCache.DEFAULT_MAX_CACHE_SIZE);
    }
    
    /*
    /**********************************************************
    /* Cache accessors
    /**********************************************************
     */
    
    @Override
    public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
        return new LRUMap<>(Math.min(64, _deserializerCacheSize >> 2), _deserializerCacheSize);
    }

    /*
    /**********************************************************
    /* Configuration using Builder
    /**********************************************************
     */

    /**
     * @return Builder instance to construct {@link DefaultCacheProvider} instance.
     */
    public static DefaultCacheProvider.Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to construct {@link DefaultCacheProvider} instance
     * and to keep {@link DefaultCacheProvider} immutable.
     */
    public static class Builder {

        /**
         * Size of {@link LookupCache} instance to create when {@link #forDeserializerCache(DeserializationConfig)}
         * is invoked.
         * Counter part of {@link DefaultCacheProvider#_deserializerCacheSize}.
         */
        private int _deserializerCacheSize;
        
        private Builder() { }

        /**
         * @param deserializerCacheSize Size of {@link LookupCache} instance to create when
         *    {@link #forDeserializerCache(DeserializationConfig)} is invoked
         * @return this builder
         * @throws IllegalArgumentException if the {@code deserializerCacheSize} is negative value
         * @since 2.16
         */
        public Builder deserializerCacheSize(int deserializerCacheSize) {
            if (deserializerCacheSize < 0) {
                throw new IllegalArgumentException("Cannot pass negative value for deserializerCacheSize.");
            }
            _deserializerCacheSize = deserializerCacheSize;
            return this;
        }

        /**
         * Constructs and returns a {@link DefaultCacheProvider} instance with given configuration.
         * If any of the configuration is not set, it will use default configuration.
         * 
         * @return Constructs and returns a {@link DefaultCacheProvider} instance with given configuration
         */
        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_deserializerCacheSize);
        }

    }
}

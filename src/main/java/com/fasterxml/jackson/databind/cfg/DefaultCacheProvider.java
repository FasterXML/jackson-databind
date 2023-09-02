package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LookupCache;

import java.util.Objects;

/**
 * Default implementation of {@link CacheProvider}.
 * Provides builder-based custom cache configuration using {@link DefaultCacheProvider.Builder}.
 * Users can either use {@link DefaultCacheProvider.Builder} or write their own {@link CacheProvider} imlementation.
 * <p>
 * WARNING: Configured cache instances using {@link DefaultCacheProvider.Builder} are "shared".
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
     * {@link LookupCache} instance to be used create {@link DeserializerCache} instance when {@link #forDeserializerCache} is invoked.
     */
    protected final LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;
    
    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    protected DefaultCacheProvider(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache)
    {
        _deserializerCache = deserializerCache;
    }
    
    /*
    /**********************************************************************
    /* Defaults
    /**********************************************************************
     */

    /**
     * @return Default {@link DefaultCacheProvider} instance for default configuration and null-safety.
     */
    public static CacheProvider defaultInstance() {
        return new DefaultCacheProvider(DeserializerCache.defaultCache());
    }
    
    /*
    /**********************************************************
    /* Cache accessors
    /**********************************************************
     */
    
    @Override
    public DeserializerCache forDeserializerCache(DeserializationConfig config) {
        return new DeserializerCache(_deserializerCache);
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
         * Counter part of {@link DefaultCacheProvider#_deserializerCache}.
         */
        private LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;
        
        protected Builder() { }

        /**
         * Fluent API for configuring {@link DefaultCacheProvider#_deserializerCache} instance for {@link DefaultCacheProvider}.
         * 
         * @param deserializerCache {@link LookupCache} instance to be used create {@link DeserializerCache} instance.
         * @throws IllegalArgumentException if {@code deserializerCache} is null.
         */
        public Builder deserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache) {
            if (deserializerCache == null) {
                throw new IllegalArgumentException("Cannot pass null deserializerCache");
            }
            _deserializerCache = deserializerCache;
            return this;
        }

        /**
         * Constructs and returns a {@link DefaultCacheProvider} instance with given configuration.
         * If any of the configuration is not set, it will use default configuration.
         * 
         * @return Constructs and returns a {@link DefaultCacheProvider} instance with given configuration.
         */
        public DefaultCacheProvider build() {
            LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache = Objects.isNull(_deserializerCache) 
                    ? DeserializerCache.defaultCache() : _deserializerCache;
            
            return new DefaultCacheProvider(deserializerCache);
        }

    }
}

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * Default implementation of {@link CacheProvider}.
 * Provides builder-based custom cache configuration using {@link DefaultCacheProvider.Builder}.
 * <p>
 * Users can either use {@link DefaultCacheProvider.Builder} or write their own {@link CacheProvider} imlementation.
 * 
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider
{
    private static final long serialVersionUID = 1L;

    /**
     * Cache instance to provide for {@link DeserializerCache}.
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
    public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
        return _deserializerCache;
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

        public Builder deserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache) {
            _deserializerCache = deserializerCache;
            return this;
        }

        /**
         * @return Constructs and returns a {@link DefaultCacheProvider} instance with given configuration.
         */
        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_deserializerCache);
        }

    }
}

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * Default implementation of {@link CacheProvider} that provides easy, builder-based custom cache configuration
 * using {@link DefaultCacheProvider.Builder}.
 * <p>
 * Users can either use this class or implement their own {@link CacheProvider} imlementation.
 * 
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider
{

    private static final long serialVersionUID = 1L;

    /**
     * Cache instance to be used by {@link DeserializerCache}.
     */
    protected final LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;

    protected DefaultCacheProvider(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache)
    {
        _deserializerCache = deserializerCache;
    }

    public static CacheProvider defaultInstance() {
        return new DefaultCacheProvider(DeserializerCache.defaultCache());
    }
    
    /*
    /**********************************************************
    /* Cache accessors
    /**********************************************************
     */
    
    @Override
    public LookupCache<JavaType, JsonDeserializer<Object>> provideDeserializerCache() {
        return _deserializerCache;
    }

    /*
    /**********************************************************
    /* Configuration using Builder
    /**********************************************************
     */

    public static DefaultCacheProvider.Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to construct {@link DefaultCacheProvider} instance
     * and to keep {@link DefaultCacheProvider} immutable.
     */
    protected static class Builder {

        /**
         * Supplier of cache instance to be used by {@link DeserializerCache}.
         */
        private LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;
        
        protected Builder() { }

        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_deserializerCache);
        }
        
        public Builder deserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache) {
            _deserializerCache = deserializerCache;
            return this;
        }
    }
}

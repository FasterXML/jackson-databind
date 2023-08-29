package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * Container for {@link LookupCache} instances to use to override default cache implementations used.
 * Should only be configured via {@link com.fasterxml.jackson.databind.json.JsonMapper.Builder#cacheProvider(DefaultCacheProvider)}.
 * 
 * @since 2.16
 */
public class DefaultCacheProvider
    implements CacheProvider, java.io.Serializable
{
    private static final long serialVersionUID = 1L; // 2.6

    protected LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;

    protected DefaultCacheProvider() { }
    
    protected DefaultCacheProvider setDeserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> cache) {
        _deserializerCache = cache;
        return this;
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
        return new Builder(new DefaultCacheProvider());
    }

    public static class Builder {
        
        protected final DefaultCacheProvider cacheProvider;

        public Builder(DefaultCacheProvider cacheProvider) {
            this.cacheProvider = cacheProvider;
        }

        public DefaultCacheProvider build() {
            return cacheProvider;
        }
        
        public Builder forDeserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> cache) {
            cacheProvider.setDeserializerCache(cache);
            return this;
        }
    }
}

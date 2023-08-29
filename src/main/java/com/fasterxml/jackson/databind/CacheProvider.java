package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * Container for {@link LookupCache} instances to use to override default cache implementations used.
 * Should only be configured via {@link com.fasterxml.jackson.databind.json.JsonMapper.Builder#cacheProvider(CacheProvider)}.
 * 
 * @since 2.16
 */
public class CacheProvider
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L; // 2.6

    protected LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;

    protected CacheProvider() { }
    
    protected CacheProvider setDeserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> cache) {
        _deserializerCache = cache;
        return this;
    }

    /*
    /**********************************************************
    /* Builder Initialization
    /**********************************************************
     */

    public static CacheProvider.Builder builder() {
        return new Builder(new CacheProvider());
    }

    public LookupCache<JavaType, JsonDeserializer<Object>> provideForDeserializerCache() {
        return _deserializerCache;
    }

    public static class Builder {
        
        protected final CacheProvider cacheProvider;

        public Builder(CacheProvider cacheProvider) {
            this.cacheProvider = cacheProvider;
        }

        public CacheProvider build() {
            return cacheProvider;
        }
        
        /*
        /**********************************************************
        /* Configuration using Builder
        /**********************************************************
         */
        
        public Builder forDeserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> cache) {
            cacheProvider.setDeserializerCache(cache);
            return this;
        }
    }
}

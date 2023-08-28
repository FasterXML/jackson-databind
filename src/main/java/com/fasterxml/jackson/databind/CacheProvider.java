package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.util.LookupCache;

public class CacheProvider {

    protected LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCache;

    protected CacheProvider() {
    }
    
    protected CacheProvider forDeserializerCache(LookupCache<JavaType, JsonDeserializer<Object>> cache) {
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
            cacheProvider._deserializerCache = cache;
            return this;
        }
    }
}

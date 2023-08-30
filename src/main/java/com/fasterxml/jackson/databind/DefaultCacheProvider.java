package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.util.LookupCache;

import java.util.function.Supplier;

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
    protected final LookupCache<JavaType, JsonDeserializer<Object>> _deserializerCacheSupplier;

    protected DefaultCacheProvider(LookupCache<JavaType, JsonDeserializer<Object>> deserializerCache)
    {
        _deserializerCacheSupplier = deserializerCache;
    }
    
    /*
    /**********************************************************
    /* Cache accessors
    /**********************************************************
     */
    
    @Override
    public LookupCache<JavaType, JsonDeserializer<Object>> provideDeserializerCache() {
        return _deserializerCacheSupplier;
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
    public static class Builder {

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

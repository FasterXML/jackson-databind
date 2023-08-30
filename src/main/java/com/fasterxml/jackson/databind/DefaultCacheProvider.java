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
     * Supplier of cache instance to be used by {@link DeserializerCache}.
     */
    protected final Supplier<LookupCache<JavaType, JsonDeserializer<Object>>> _deserializerCacheSupplier;

    protected DefaultCacheProvider(Supplier<LookupCache<JavaType, JsonDeserializer<Object>>> deserializerCache)
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
        return _deserializerCacheSupplier.get();
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
        private Supplier<LookupCache<JavaType, JsonDeserializer<Object>>> _deserializerCacheSupplier;
        
        protected Builder() { }

        public DefaultCacheProvider build() {
            return new DefaultCacheProvider(_deserializerCacheSupplier);
        }
        
        public Builder deserializerCache(Supplier<LookupCache<JavaType, JsonDeserializer<Object>>> deserializerCache) {
            _deserializerCacheSupplier = deserializerCache;
            return this;
        }
    }
}

package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.util.LookupCache;

/**
 * Interface that defines API for Cache configurations.
 * 
 * @since 2.16
 */
public interface CacheProvider {
    
    LookupCache<JavaType, JsonDeserializer<Object>> provideDeserializerCache();
    
}

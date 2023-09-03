package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LookupCache;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <a href="https://github.com/FasterXML/jackson-databind/issues/2502">
 * [databind#2502] Test for adding a way to configure Caches Jackson uses</a>
 *
 * @since 2.16
 */
public class CacheProviderTest
{
    
    static class RandomBean {
        public int point;
    }

    static class AnotherBean {
        public int height;
    }

    static class SimpleTestCache implements LookupCache<JavaType, JsonDeserializer<Object>> {

        final HashMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers;

        public SimpleTestCache(int cacheSize) {
            _cachedDeserializers = new HashMap<>(cacheSize);
            createdCaches.add(_cachedDeserializers);
        }

        @Override
        public int size() {
            return _cachedDeserializers.size();
        }

        @Override
        public JsonDeserializer<Object> get(Object key) {
            invoked = true;
            return _cachedDeserializers.get(key);
        }

        @Override
        public JsonDeserializer<Object> put(JavaType key, JsonDeserializer<Object> value) {
            invoked = true;
            return _cachedDeserializers.put(key, value);
        }

        @Override
        public JsonDeserializer<Object> putIfAbsent(JavaType key, JsonDeserializer<Object> value) {
            invoked = true;
            return _cachedDeserializers.putIfAbsent(key, value);
        }

        @Override
        public void clear() {
            _cachedDeserializers.clear();
        }
    }
    
    static class CustomCacheProvider implements CacheProvider {
        @Override
        public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
            invoked = true;
            return new SimpleTestCache(123);
        }
    }
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */

    // For checking CustomCacheProvider.forDeserializerCache() was actually used
    private static boolean invoked = false;

    // For checking the cache was actually created
    private static final List<Map<JavaType, JsonDeserializer<Object>>> createdCaches = new ArrayList<>();
    
    @Before
    public void setUp() {
        createdCaches.clear();
        invoked = false;
    }

    @Test
    public void testDefaultCacheProviderConfig() throws Exception
    {
        CacheProvider cacheProvider = DefaultCacheProvider.builder()
                .deserializerCacheSize(1234)
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider).build();

        assertNotNull(mapper.readValue("{\"point\":24}", RandomBean.class));
    }

    @Test
    public void testCustomCacheProviderConfig() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(new CustomCacheProvider())
                .build();

        assertTrue(invoked);
        assertNotNull(mapper.readValue("{\"point\":24}", RandomBean.class));
    }

    @Test
    public void testDefaultCacheProviderSharesCache() throws Exception
    {
        // Arrange
        // 1. shared CacheProvider
        CacheProvider cacheProvider = new CustomCacheProvider();
        
        // 2. two different mapper instances
        ObjectMapper mapper1 = JsonMapper.builder()
                .cacheProvider(cacheProvider)
                .build();
        ObjectMapper mapper2 = JsonMapper.builder()
                .cacheProvider(cacheProvider)
                .build();
        
        // Act 
        // 3. Add two different types to each mapper cache
        mapper1.readValue("{\"point\":24}", RandomBean.class);
        mapper2.readValue("{\"height\":24}", AnotherBean.class);
        
        // Assert
        // 4. Should have created two cache instance
        assertEquals(2, createdCaches.size());
    }


    @Test
    public void testBuilderNullCheckingForDeserializerCacheConfig() throws Exception
    {
        try {
            DefaultCacheProvider.builder()
                    .deserializerCacheSize(-1);
            fail("Should not reach here");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot set deserializerCacheSize to a negative value"));
        }
    }

    @Test
    public void testBuilderBuildWithDefaults() throws Exception
    {
        try {
            DefaultCacheProvider.builder().build();
        } catch (IllegalArgumentException e) {
            fail("Should not reach here");
        }
    }
}

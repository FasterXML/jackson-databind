package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LRUMap;
import com.fasterxml.jackson.databind.util.LookupCache;
import com.fasterxml.jackson.databind.util.TypeKey;
import org.junit.Test;

import java.util.HashMap;

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
    
    static class SerBean {
        public int slide = 123;
    }

    static class SimpleTestCache implements LookupCache<JavaType, JsonDeserializer<Object>> {

        final HashMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers;
        
        boolean invokedAtLeastOnce = false;

        public SimpleTestCache(int cacheSize) {
            _cachedDeserializers = new HashMap<>(cacheSize);
        }
        
        @Override
        public int size() {
            return _cachedDeserializers.size();
        }

        @Override
        public JsonDeserializer<Object> get(Object key) {
            invokedAtLeastOnce = true;
            return _cachedDeserializers.get(key);
        }

        @Override
        public JsonDeserializer<Object> put(JavaType key, JsonDeserializer<Object> value) {
            invokedAtLeastOnce = true;
            return _cachedDeserializers.put(key, value);
        }

        @Override
        public JsonDeserializer<Object> putIfAbsent(JavaType key, JsonDeserializer<Object> value) {
            invokedAtLeastOnce = true;
            return _cachedDeserializers.putIfAbsent(key, value);
        }

        @Override
        public void clear() {
            _cachedDeserializers.clear();
        }

        boolean isInvokedAtLeastOnce() {
            return invokedAtLeastOnce;
        }
    }
    
    static class CustomCacheProvider implements CacheProvider {
        
        final SimpleTestCache _cache;
        int createCacheCount = 0;
        
        public CustomCacheProvider(SimpleTestCache cache) {
            _cache = cache;
        }

        @Override
        public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
            createCacheCount++;
            return _cache;
        }

        @Override
        public LookupCache<TypeKey, JsonSerializer<Object>> forSerializerCache(SerializationConfig config) {
            return new LRUMap<>(8, 64);
        }

        int createCacheCount() {
            return createCacheCount;
        }
    }

    static class CustomSerializerCacheProvider implements CacheProvider {

        final CustomTestSerializerCache _cache;

        public CustomSerializerCacheProvider(CustomTestSerializerCache cache) {
            _cache = cache;
        }

        @Override
        public LookupCache<JavaType, JsonDeserializer<Object>> forDeserializerCache(DeserializationConfig config) {
            return new LRUMap<>(16, 64);
        }

        @Override
        public LookupCache<TypeKey, JsonSerializer<Object>> forSerializerCache(SerializationConfig config) {
            return _cache;
        }
    }

    static class CustomTestSerializerCache extends LRUMap<TypeKey, JsonSerializer<Object>> {

        public CustomTestSerializerCache() {
            super(8, 64);
        }
    } 
    
    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */
    
    @Test
    public void testDefaultCacheProviderConfigDeserializerCache() throws Exception
    {
        CacheProvider cacheProvider = DefaultCacheProvider.builder()
                .maxDeserializerCacheSize(1234)
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider).build();

        assertNotNull(mapper.readValue("{\"point\":24}", RandomBean.class));
    }

    @Test
    public void testDefaultCacheProviderConfigDeserializerCacheSizeZero() throws Exception
    {
        CacheProvider cacheProvider = DefaultCacheProvider.builder()
                .maxDeserializerCacheSize(0)
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider)
                .build();

        assertNotNull(mapper.readValue("{\"point\":24}", RandomBean.class));
    }

    @Test
    public void testBuilderNullCheckingForDeserializerCacheConfig() throws Exception
    {
        try {
            DefaultCacheProvider.builder()
                    .maxDeserializerCacheSize(-1);
            fail("Should not reach here");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot set maxDeserializerCacheSize to a negative value"));
        }
    }

    @Test
    public void testCustomCacheProviderConfig() throws Exception
    {
        SimpleTestCache cache = new SimpleTestCache(123);
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(new CustomCacheProvider(cache))
                .build();

        assertNotNull(mapper.readValue("{\"point\":24}", RandomBean.class));
        assertTrue(cache.isInvokedAtLeastOnce());
    }

    @Test
    public void testDefaultCacheProviderSharesCache() throws Exception
    {
        // Arrange
        // 1. shared CacheProvider
        CustomCacheProvider cacheProvider = new CustomCacheProvider(new SimpleTestCache(123));
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
        assertEquals(2, cacheProvider.createCacheCount());
    }

    @Test
    public void testBuilderBuildWithDefaults() throws Exception
    {
        // does not throw
        DefaultCacheProvider.builder().build();
    }

    @Test
    public void testDefaultCacheProviderConfigSerializerCache() throws Exception
    {
        CacheProvider cacheProvider = DefaultCacheProvider.builder()
                .maxSerializerCacheSize(1234)
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider).build();
        
        String json = mapper.writeValueAsString(new SerBean());
        
        assertEquals("{\"slide\":123}", json);
    }

    @Test
    public void testDefaultCacheProviderConfigSerializerCacheSizeZero() throws Exception
    {
        CacheProvider cacheProvider = DefaultCacheProvider.builder()
                .maxSerializerCacheSize(0)
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider)
                .build();

        String json = mapper.writeValueAsString(new SerBean());
        
        assertEquals("{\"slide\":123}", json);
    }

    @Test
    public void testBuilderNullCheckingForSerializerCacheConfig() throws Exception
    {
        try {
            DefaultCacheProvider.builder()
                    .maxSerializerCacheSize(-1);
            fail("Should not reach here");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot set maxSerializerCacheSize to a negative value"));
        }
    }

    @Test
    public void testCustomCacheProviderSerializerCacheConfig() throws Exception
    {
        CustomTestSerializerCache cache = new CustomTestSerializerCache();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(new CustomSerializerCacheProvider(cache))
                .build();

        String json = mapper.writeValueAsString(new SerBean());

        assertEquals("{\"slide\":123}", json);
    }
}

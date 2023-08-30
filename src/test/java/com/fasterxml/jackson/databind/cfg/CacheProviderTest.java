package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LookupCache;
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

    static class SimpleTestCache implements LookupCache<JavaType, JsonDeserializer<Object>> {

        final HashMap<JavaType, JsonDeserializer<Object>> cache = new HashMap<>();
        // Checking the cache was actually used
        boolean invoked = false;

        @Override
        public int size(){
            return cache.size();
        }

        @Override
        public JsonDeserializer<Object> get(Object key) {
            invoked = true;
            return cache.get(key);
        }

        @Override
        public JsonDeserializer<Object> put(JavaType key, JsonDeserializer<Object> value) {
            invoked = true;
            return cache.put(key, value);
        }

        @Override
        public JsonDeserializer<Object> putIfAbsent(JavaType key, JsonDeserializer<Object> value) {
            invoked = true;
            return cache.putIfAbsent(key, value);
        }

        @Override
        public void clear() {
            cache.clear();
        }
    }

    @Test
    public void testCacheConfig() throws Exception
    {
        LookupCache<JavaType, JsonDeserializer<Object>> cache = new SimpleTestCache();
        DefaultCacheProvider cacheProvider = DefaultCacheProvider.builder()
                .forDeserializerCache(cache)
                .build();
        ObjectMapper mapper = JsonMapper.builder().cacheProvider(cacheProvider).build();

        RandomBean bean = mapper.readValue("{\"point\":24}", RandomBean.class);
        assertEquals(24, bean.point);
        assertTrue(((SimpleTestCache) cache).invoked);
    }
}

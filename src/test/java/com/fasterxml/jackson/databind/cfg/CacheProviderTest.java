package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LookupCache;
import org.junit.Test;

import java.util.Map;

public class CacheProviderTest extends BaseMapTest {
    
    static class RandomBean {
        public int point;
    }

    static class SimpleExampleCache implements LookupCache<JavaType, JsonDeserializer<Object>> {
        int invocationCount = 0;
        final Map<JavaType, JsonDeserializer<Object>> cache = new java.util.HashMap<>();

        @Override
        public int size() {
            return cache.size();
        }

        @Override
        public JsonDeserializer<Object> get(Object key) {
            return cache.get(key);
        }

        @Override
        public JsonDeserializer<Object> put(JavaType key, JsonDeserializer<Object> value) {
            return cache.put(key, value);
        }

        @Override
        public JsonDeserializer<Object> putIfAbsent(JavaType key, JsonDeserializer<Object> value) {
            return cache.putIfAbsent(key, value);
        }

        @Override
        public void clear() {
            cache.clear();
        }
    }

    @Test
    public void testCacheConfig() throws Exception {
        CacheProvider cacheProvider = CacheProvider.builder()
                .forDeserializerCache(new SimpleExampleCache())
                .build();
        
        ObjectMapper mapper = JsonMapper.builder().cacheProvider(cacheProvider).build();
        
        RandomBean bean = mapper.readValue("{\"point\":24}", RandomBean.class);
        assertEquals(24, bean.point);
    }
}

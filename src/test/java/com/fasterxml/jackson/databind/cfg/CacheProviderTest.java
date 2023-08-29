package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LookupCache;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        @Override
        public int size(){
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
    public void testCacheConfig() throws Exception
    {
        CacheProvider cacheProvider = CacheProvider.builder()
                .forDeserializerCache(new SimpleTestCache())
                .build();

        ObjectMapper mapper = JsonMapper.builder().cacheProvider(cacheProvider).build();

        RandomBean bean = mapper.readValue("{\"point\":24}", RandomBean.class);
        assertEquals(24, bean.point);
    }
}

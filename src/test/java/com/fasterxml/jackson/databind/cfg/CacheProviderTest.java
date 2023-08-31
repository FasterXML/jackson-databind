package com.fasterxml.jackson.databind.cfg;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.LookupCache;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <a href="https://github.com/FasterXML/jackson-databind/issues/2502">
 * [databind#2502] Test for adding a way to configure Caches Jackson uses</a>
 *
 * @since 2.16
 */
public class CacheProviderTest {

    static class RandomBean {
        public int point;
    }

    static class SimpleTestCache implements LookupCache<JavaType, JsonDeserializer<Object>> {

        final HashMap<JavaType, JsonDeserializer<Object>> cache = new HashMap<>();

        @Override
        public int size() {
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

    // Checking the cache was actually used
    private static boolean invoked = false;

    @Test
    public void testCacheConfig() throws Exception {
        DefaultCacheProvider cacheProvider = DefaultCacheProvider.builder()
                .deserializerCache(new SimpleTestCache())
                .build();
        ObjectMapper mapper = JsonMapper.builder()
                .cacheProvider(cacheProvider).build();

        RandomBean bean = mapper.readValue("{\"point\":24}", RandomBean.class);
        assertEquals(24, bean.point);
        assertTrue(invoked);
    }
}

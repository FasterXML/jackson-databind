package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UnlimitedLookupCacheTest {
    @Test
    public void testCache() {
        UnlimitedLookupCache<Long, String> cache = new UnlimitedLookupCache<>(4, 10);
        assertNull(cache.get(1000L));
        assertNull(cache.put(1000L, "Thousand"));
        assertEquals("Thousand", cache.get(1000L));
        assertEquals("Thousand", cache.putIfAbsent(1000L, "Míle"));
        assertEquals("Thousand", cache.get(1000L));
        assertEquals("Thousand", cache.put(1000L, "Míle"));
        assertEquals("Míle", cache.get(1000L));
        cache.clear();
        assertNull(cache.put(1000L, "Thousand"));
    }

    @Test
    public void testCompatibility() throws JsonProcessingException {
        UnlimitedLookupCache<Object, JavaType> cache = new UnlimitedLookupCache<>(4, 10);
        TypeFactory tf = TypeFactory.defaultInstance().withCache(cache);

        //TODO find way to inject the `tf` instance into an ObjectMapper (via MapperBuilder?)

        //ObjectMapper mapper = new ObjectMapper();
        //mapper.setTypeFactory(tf);
        //assertEquals("1000", mapper.writeValueAsString(1000));
    }
}

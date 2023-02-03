package com.fasterxml.jackson.failing;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for [#171]
public class TestUnwrappedMap171 extends BaseMapTest
{
    static class MapUnwrap {

        public MapUnwrap() { }
        public MapUnwrap(String key, Object value) {
            map = Collections.singletonMap(key, value);
        }

        @JsonUnwrapped(prefix="map.")
        public Map<String, Object> map;
    }

    // // // Reuse mapper to keep tests bit faster

    private final ObjectMapper MAPPER = new ObjectMapper();

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    public void testMapUnwrapSerialize() throws Exception
    {
        String json = MAPPER.writeValueAsString(new MapUnwrap("test", 6));
        assertEquals("{\"map.test\": 6}", json);
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    public void testMapUnwrapDeserialize() throws Exception
    {
        MapUnwrap root = MAPPER.readValue("{\"map.test\": 6}", MapUnwrap.class);

        assertEquals(1, root.map.size());
        assertEquals(6, ((Number)root.map.get("test")).intValue());
    }
}

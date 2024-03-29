package com.fasterxml.jackson.failing;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests for [#171]
class TestUnwrappedMap171 extends DatabindTestUtil {
    static class MapUnwrap {

        public MapUnwrap() {
        }

        public MapUnwrap(String key, Object value) {
            map = Collections.singletonMap(key, value);
        }

        @JsonUnwrapped(prefix = "map.")
        public Map<String, Object> map;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void mapUnwrapSerialize() throws Exception {
        String json = MAPPER.writeValueAsString(new MapUnwrap("test", 6));
        assertEquals("{\"map.test\": 6}", json);
    }

    @Test
    void mapUnwrapDeserialize() throws Exception {
        MapUnwrap root = MAPPER.readValue("{\"map.test\": 6}", MapUnwrap.class);

        assertEquals(1, root.map.size());
        assertEquals(6, ((Number) root.map.get("test")).intValue());
    }
}

package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapInclusion1649Test extends DatabindTestUtil {
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    static class Bean1649 {
        public Map<String, String> map;

        public Bean1649(String key, String value) {
            map = new LinkedHashMap<>();
            map.put(key, value);
        }
    }

    final private ObjectMapper MAPPER = objectMapper();

    // [databind#1649]
    @Test
    void nonEmptyViaClass() throws IOException {
        // non-empty/null, include
        assertEquals(a2q("{'map':{'a':'b'}}"),
                MAPPER.writeValueAsString(new Bean1649("a", "b")));
        // null, empty, nope
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new Bean1649("a", null)));
        assertEquals(a2q("{}"),
                MAPPER.writeValueAsString(new Bean1649("a", "")));
    }
}

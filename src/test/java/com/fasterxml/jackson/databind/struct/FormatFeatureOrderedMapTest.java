package com.fasterxml.jackson.databind.struct;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonFormat} and specifically <code>JsonFormat.Feature</code>s.
 */
public class FormatFeatureOrderedMapTest extends DatabindTestUtil
{
    static class SortedKeysMap {
        @JsonFormat(with = JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES)
        public Map<String,Integer> values = new LinkedHashMap<>();

        protected SortedKeysMap() { }

        public SortedKeysMap put(String key, int value) {
            values.put(key, value);
            return this;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#1232]: allow forcing sorting on Map keys
    @Test
    public void testOrderedMaps() throws Exception {
        SortedKeysMap map = new SortedKeysMap()
            .put("b", 2)
            .put("a", 1);
        assertEquals(a2q("{'values':{'a':1,'b':2}}"),
                MAPPER.writeValueAsString(map));
    }
}

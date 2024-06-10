package com.fasterxml.jackson.databind.util;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CompactStringObjectMapTest extends DatabindTestUtil
{
    @Test
    public void testBig()
    {
        Map<String,String> all = new LinkedHashMap<>();
        for (int i = 0; i < 1000; ++i) {
            String key = "key"+i;
            all.put(key, key);
        }
        CompactStringObjectMap map = CompactStringObjectMap.construct(all);
        assertEquals(1000, map.keys().size());

        for (String key : all.keySet()) {
            assertEquals(key, map.find(key));
        }

        // and then bogus empty keys
        assertNull(map.find("key1000"));
        assertNull(map.find("keyXXX"));
        assertNull(map.find(""));
    }
}

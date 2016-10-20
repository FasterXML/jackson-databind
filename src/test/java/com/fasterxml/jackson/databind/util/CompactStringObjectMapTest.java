package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.databind.BaseMapTest;

public class CompactStringObjectMapTest extends BaseMapTest
{
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

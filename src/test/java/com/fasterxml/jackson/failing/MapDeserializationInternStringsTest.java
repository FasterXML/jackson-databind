package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.InternCache;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// for [core#946]
public class MapDeserializationInternStringsTest extends BaseMapTest
{
    public void testArbitraryKeysDontChurnCache() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = a2q("{'1st':'onedata','2nd':'twodata'}");
        Map<String, String> expected = new HashMap<>();
        expected.put("1st", "onedata");
        expected.put("2nd", "twodata");
        // Clear the InternCache before deserialization to avoid impact from other tests. Note that the
        // cache is static state, and this test will not be meaningful in an environment where tests are
        // executed in parallel.
        InternCache.instance.clear();
        Map<String, String> deserialized = mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        // Verify our test has done what we expect before asserting state on the InternCache.
        assertEquals(expected, deserialized);
        assertEquals("Arbitrary map key values should not be added to the InternCache because " +
                "map keys may have greater cardinality than the InternCache expects",
                Collections.emptyMap(), InternCache.instance);
    }
}

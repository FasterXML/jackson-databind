package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NestedCollectionsTest extends BaseMapTest
{
    private final static ObjectMapper MAPPER = newJsonMapper();

    // Tests from [databind#4149] to show problems wrt [databind#4122]

    public void testMapOfLists() throws Exception
    {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(3, 4);
        HashMap<Object, Object> src = new HashMap<>();
        src.put("key1", l1);
        src.put("key2", l2);
        String json = MAPPER.writeValueAsString(src);
        assertEquals(src, MAPPER.readValue(json,
                new TypeReference<Map<?, List<?>>>() {}));
    }

    public void testMapOfMaps() throws Exception
    {
        HashMap<Object, Object> src = new HashMap<>();
        HashMap<Object, Object> innerMap = new HashMap<>();
        innerMap.put("X", "Y");
        src.put("1", innerMap);
        String json = MAPPER.writeValueAsString(src);
        assertEquals(src, MAPPER.readValue(json, new TypeReference<Map<?, Map<?, ?>>>() {}));
    }    

}

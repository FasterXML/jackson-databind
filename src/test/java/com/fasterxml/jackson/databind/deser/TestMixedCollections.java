package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// tests based on tests in vavr-jackson
public class TestMixedCollections extends BaseMapTest {

    public void testMapOfLists() throws IOException {
        HashMap<Object, Object> src = new HashMap<>();
        ArrayList<Integer> l1 = new ArrayList<>();
        l1.add(1);
        l1.add(2);
        ArrayList<Integer> l2 = new ArrayList<>();
        l2.add(3);
        l2.add(4);
        src.put("key1", l1);
        src.put("key2", l2);
        ObjectMapper mapper = newJsonMapper();
        String json = mapper.writer().writeValueAsString(src);
        assertEquals(src, mapper.readValue(json, new TypeReference<Map<?, List<?>>>() {}));
    }

    public void testMapOfMaps() throws IOException {
        HashMap<Object, Object> src = new HashMap<>();
        HashMap<Object, Object> innerMap = new HashMap<>();
        innerMap.put("X", "Y");
        src.put("1", innerMap);
        ObjectMapper mapper = newJsonMapper();
        String json = mapper.writer().writeValueAsString(src);
        assertEquals(src, mapper.readValue(json, new TypeReference<Map<?, Map<?, ?>>>() {}));
    }
}

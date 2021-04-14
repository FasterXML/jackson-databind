package com.fasterxml.jackson.databind.deser.merge;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// for [databind#1844]
public class MapMerge1844Test extends BaseMapTest
{
    static class TestMap1844 {
        public Map<String, Integer> getMapStringInteger() {
            return mapStringInteger;
        }

        @JsonProperty("key1")
        public void setMapStringInteger(Map<String, Integer> mapStringInteger) {
            this.mapStringInteger = mapStringInteger;
        }

        public Map<Integer, Integer> getMapIntegerInteger() {
            return mapIntegerInteger;
        }

        @JsonProperty("key2")
        public void setMapIntegerInteger(Map<Integer, Integer> mapIntegerInteger) {
            this.mapIntegerInteger = mapIntegerInteger;
        }

        private Map<String, Integer> mapStringInteger = new LinkedHashMap<>();

        private Map<Integer, Integer> mapIntegerInteger = new LinkedHashMap<>();
    }

    // for [databind#1844]
    public void testMap1844() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        mapper.setDefaultMergeable(true);

        final String f1 = a2q(
"{ 'key1' : {\n"
+"  '1': 1, '2': 2, '3': 3\n"
+"}, 'key2': {\n"
+"  '1': 1, '2': 2, '3': 3\n"
+"} }"
);
        final String f2 = a2q(
"{ 'key1' : {\n"
+"  '1': 2, '2': 3, '4': 5\n"
+"}, 'key2': {\n"
+"  '1': 2, '2': 3, '4': 5\n"
+"} }"
);
        TestMap1844 testMap = mapper.readerFor(TestMap1844.class).readValue(f1);
        testMap = mapper.readerForUpdating(testMap).readValue(f2);

        assertEquals(Integer.valueOf(2), testMap.getMapStringInteger().get("1"));
        assertEquals(Integer.valueOf(3), testMap.getMapStringInteger().get("2"));
        assertEquals(Integer.valueOf(3), testMap.getMapStringInteger().get("3"));
        assertEquals(Integer.valueOf(5), testMap.getMapStringInteger().get("4"));

        assertEquals(Integer.valueOf(2), testMap.getMapIntegerInteger().get(1));
        assertEquals(Integer.valueOf(3), testMap.getMapIntegerInteger().get(2));
        assertEquals(Integer.valueOf(3), testMap.getMapIntegerInteger().get(3));
        assertEquals(Integer.valueOf(5), testMap.getMapIntegerInteger().get(4));
    }
}
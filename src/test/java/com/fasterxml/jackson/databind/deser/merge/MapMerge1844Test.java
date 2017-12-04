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

        private Map<String, Integer> mapStringInteger = new HashMap<>();

        private Map<Integer, Integer> mapIntegerInteger = new HashMap<>();
    }

    // for [databind#1844]
    public void testMap1844() throws Exception
    {
        final ObjectMapper mapper = newObjectMapper();
        mapper.setDefaultMergeable(true);

        TestMap1844 testMap = new TestMap1844();
        TestMap1844 testMap1 = new TestMap1844();

        final String f1 = aposToQuotes(
"{ 'key1' : {\n"
+"  '1': 1, '2': 2, '3': 3\n"
+"}, 'key2': {\n"
+"  '1': 1, '2': 2, '3': 3\n"
+"} }"
);
        final String f2 = aposToQuotes(
"{ 'key1' : {\n"
+"  '1': 2, '2': 3, '3': 5\n"
+"}, 'key2': {\n"
+"  '1': 2, '2': 3, '3': 5\n"
+"} }"
);
        
        testMap = mapper.readerForUpdating(testMap).readValue(f1);
        testMap1 = mapper.readerForUpdating(testMap1).readValue(f1);
        testMap = mapper.readerForUpdating(testMap).readValue(f2);
        testMap1 = mapper.readerForUpdating(testMap1).readValue(f2);
//        System.out.println(testMap.getMapIntegerInteger().get(1));
//        System.out.println(testMap.getMapStringInteger().get("1"));
        assertEquals(testMap.getMapIntegerInteger().get(1), testMap1.getMapIntegerInteger().get(1));
        assertEquals(testMap.getMapStringInteger().get("1"), testMap1.getMapStringInteger().get("1"));
    }
}
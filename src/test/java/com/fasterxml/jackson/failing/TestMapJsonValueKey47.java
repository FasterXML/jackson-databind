package com.fasterxml.jackson.failing;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// [Issue#47]
public class TestMapJsonValueKey47 extends BaseMapTest
{
    public static class Wat
    {
        private final String wat;

        @JsonCreator
        Wat(String wat) {
            this.wat = wat;
        }

        @JsonValue
        public String getWat() {
            return wat;
        }

        @Override
        public String toString() {
            return "(String)[Wat: " + wat + "]";
        }
    }

    @SuppressWarnings("serial")
    static class WatMap extends HashMap<Wat,Boolean> { }
    
    public void testMapJsonValueKey()
    throws Exception
    {
        WatMap input = new WatMap();
        input.put(new Wat("3"), true);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(input);
        assertEquals(aposToQuotes("{'3':true}"), json);
    }
}

package com.fasterxml.jackson.failing;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.test.BaseTest;

public class TestMapJsonValueKey extends BaseTest
{
    public static class Wat
    {
        private final String wat;

        @JsonCreator
        Wat(String wat)
        {
            this.wat = wat;
        }

        @JsonValue
        public String getWat()
        {
            return wat;
        }

        @Override
        public String toString()
        {
            return "[Wat: " + wat + "]";
        }
    }

    public void testMapJsonValueKey()
    throws Exception
    {
        Map<Wat, Boolean> map = new HashMap<Wat, Boolean>();
        map.put(new Wat("3"), true);
        map.put(new Wat("x"), false);

        TypeReference<Map<Wat, Boolean>> type = new TypeReference<Map<Wat, Boolean>>(){};

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(map, mapper.readValue(mapper.writeValueAsBytes(map), type));
    }
}

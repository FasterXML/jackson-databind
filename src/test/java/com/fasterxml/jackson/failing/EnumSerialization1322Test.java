package com.fasterxml.jackson.failing;

import java.util.EnumMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumSerialization1322Test extends BaseMapTest
{
    // for [databind#1322]
    protected enum EnumWithJsonProperty {
        @JsonProperty("aleph")
        A;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    // [databind#1322]
    public void testEnumsWithJsonPropertyAsKey() throws Exception
    {
        EnumMap<EnumWithJsonProperty,String> input = new EnumMap<EnumWithJsonProperty,String>(EnumWithJsonProperty.class);
        input.put(EnumWithJsonProperty.A, "b");
        assertEquals("{\"aleph\":\"b\"}", MAPPER.writeValueAsString(input));
    }
}

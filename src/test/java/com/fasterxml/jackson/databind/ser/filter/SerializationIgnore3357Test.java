package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationIgnore3357Test extends BaseMapTest
{
    // [databind#3357]: Precedence of @JsonIgnore over @JsonProperty along
    // public getter with no annotations, field.
    //
    // Not 100% if this can be resolved
    static class IgnoreAndProperty3357 {
        public int toInclude = 2;

        @JsonIgnore
        @JsonProperty
        int toIgnore = 3;

        public int getToIgnore() { return toIgnore; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#3357]: Precedence of @JsonIgnore over @JsonProperty
    public void testPropertyVsIgnore3357() throws Exception
    {
        String json = MAPPER.writeValueAsString(new IgnoreAndProperty3357());
        assertEquals("{\"toInclude\":2}", json);
    }

}

package com.fasterxml.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class SerializationIgnore3357Test extends DatabindTestUtil
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
    @Test
    public void testPropertyVsIgnore3357() throws Exception
    {
        String json = MAPPER.writeValueAsString(new IgnoreAndProperty3357());
        assertEquals("{\"toInclude\":2}", json);
    }

}

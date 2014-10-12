package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestEnumSerialization572 extends BaseMapTest
{
    @JsonFormat(shape=JsonFormat.Shape.OBJECT)
    static enum PoNUM {
        A("a1"), B("b2");

        @JsonProperty
        protected final String value;
        
        private PoNUM(String v) { value = v; }

        public String getValue() { return value; }
    }

    // for [databind#572]
    static class PoOverrideAsString
    {
        @JsonFormat(shape=Shape.STRING)
    	public PoNUM value = PoNUM.B;
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();

    // [databind#572]
    public void testOverrideEnum() throws Exception
    {
    	assertEquals("{\"value\":\"B\"}", mapper.writeValueAsString(new PoOverrideAsString()));
    }
}

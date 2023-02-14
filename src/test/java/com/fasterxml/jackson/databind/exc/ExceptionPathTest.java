package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class ExceptionPathTest extends BaseMapTest
{
    static class Outer {
        public Inner inner = new Inner();
    }

    static class Inner {
        public int x;

        @JsonCreator public static Inner create(@JsonProperty("x") int x) {
            throw new RuntimeException("test-exception");
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testReferenceChainForInnerClass() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Outer());
        try {
            MAPPER.readValue(json, Outer.class);
            fail("Should not pass");
        } catch (ValueInstantiationException e) {
            String referenceStr = e.getPath().get(0).toString();
            assertEquals(getClass().getName()+"$Outer[\"inner\"]", referenceStr);
        }
    }
}

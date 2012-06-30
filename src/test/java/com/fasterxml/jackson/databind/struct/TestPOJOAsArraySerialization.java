package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.*;

public class TestPOJOAsArraySerialization extends BaseMapTest
{
    static class Pojo
    {
        @JsonFormat(shape=JsonFormat.Shape.ARRAY)
        public PojoValue value;

        public Pojo() { }
        public Pojo(String name, int x, int y, boolean c) {
            value = new PojoValue(name, x, y, c);
        }
    }

    // note: must be serialized/deserialized alphabetically; fields NOT declared in that order
    @JsonPropertyOrder(alphabetic=true)
    static class PojoValue
    {
        public int x, y;
        public String name;
        public boolean complete;

        public PojoValue() { }
        public PojoValue(String name, int x, int y, boolean c) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.complete = c;
        }
    }
    
    /*
    /*****************************************************
    /* Unit tests
    /*****************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * Test that verifies that property annotation works
     */
    public void testSimplePropertyValue() throws Exception
    {
        String json = MAPPER.writeValueAsString(new Pojo("Foobar", 42, 13, true));
        // will have wrapper POJO, then POJO-as-array..
        assertEquals("{\"value\":[true,\"Foobar\",42,13]}", json);
    }
}

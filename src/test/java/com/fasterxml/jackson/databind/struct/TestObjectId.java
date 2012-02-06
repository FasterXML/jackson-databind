package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonObjectId;

import com.fasterxml.jackson.databind.*;

public class TestObjectId extends BaseMapTest
{
    static class Identifiable
    {
        @JsonObjectId
        public String id;

        public int value;

        public Identifiable next;
        
        public Identifiable() { this(null, 0); }
        public Identifiable(String id, int v) {
            this.id = id;
            value = v;
        }
    }
    
    /*
    /*****************************************************
    /* Unit tests
    /*****************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testSimpleCyclicSerialization() throws Exception
    {
        Identifiable src = new Identifiable("x123", 13);
        src.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals("{\"id\":\"x123\",\"value\":13,\"next\":\"x123\"}", json);
    }
        
    public void testSimpleCyclicDeserialization() throws Exception
    {
        // then bring back...
        String input = "{\"id\":\"x123\",\"value\":13,\"next\":\"x123\"}";
        Identifiable result = mapper.readValue(input, Identifiable.class);
        assertEquals(13, result.value);
        assertEquals("x123", result.id);
        assertSame(result, result.next);
    }
}

package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;

public class TestObjectId extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Identifiable
    {
        public int value;

        public Identifiable next;
        
        public Identifiable() { this(0); }
        public Identifiable(int v) {
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
        Identifiable src = new Identifiable(13);
        src.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals("{\"id\":1,\"value\":13,\"next\":1}", json);
    }
        
    public void testSimpleCyclicDeserialization() throws Exception
    {
        // then bring back...
        String input = "{\"id\":1,\"value\":13,\"next\":1}";
        Identifiable result = mapper.readValue(input, Identifiable.class);
        assertEquals(13, result.value);
//        assertEquals(1, result.id);
        assertSame(result, result.next);
    }
}

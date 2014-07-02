package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class TestCreators3 extends BaseMapTest
{
    static class MultiCtor
    {
        protected String _a, _b;
        
        @JsonCreator
        MultiCtor(@JsonProperty("a") String a, @JsonProperty("b") String b) {
            _a = a;
            _b = b;
        }

        MultiCtor(String a, String b, Object c) {
            throw new RuntimeException("Wrong factory!");
        }
        
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testMulitCtor421() throws Exception
    {
        MultiCtor bean = MAPPER.readValue(aposToQuotes("{'a':'123','b':'foo'}"), MultiCtor.class);
        assertNotNull(bean);
        assertEquals("123", bean._a);
        assertEquals("foo", bean._b);
    }
}

package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests verifying handling of "virtual" conflict; case where
 * there is no actual problem, but Jackson takes too conservative view
 * on this and throws an exception.
 */
public class TestPropertyConflicts extends BaseMapTest
{
    /* We should only report an exception for cases where there is
     * real ambiguity as to how to rename things; but not when everything
     * has been explicitly defined
     */
    // [Issue#327]
    @JsonPropertyOrder({ "prop1", "prop2" })
    static class BogusConflictBean
    {
        @JsonProperty("prop1")
        public int a = 2;

        @JsonProperty("prop2")
        public int getA() {
            return 1;
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    // [Issue#327]
    public void testNonConflict() throws Exception
    {
        String json = MAPPER.writeValueAsString(new BogusConflictBean());
        assertEquals(aposToQuotes("{'prop1':2,'prop2':1}"), json);
    }
}

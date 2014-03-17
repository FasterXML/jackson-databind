package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TestNameConflicts193And327 extends BaseMapTest
{
    static class Bean193
    {
        @JsonProperty("val1")
        private int x;
        @JsonIgnore
        private int value2;
        
        public Bean193(@JsonProperty("val1")int value1,
                    @JsonProperty("val2")int value2)
        {
            this.x = value1;
            this.value2 = value2;
        }
        
        @JsonProperty("val2")
        int x()
        {
            return value2;
        }
    }

    // [Issue#323]
    static class Bean323 { 
        private int a;

        public Bean323 (@JsonProperty("a") final int a ) {
            this.a = a;
        }

        @JsonProperty("b")
        private int getA () {
            return a;
        }
    }    

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

    // [Issue#193]
    public void testIssue193() throws Exception
    {
        String json = objectWriter().writeValueAsString(new Bean193(1, 2));
        assertNotNull(json);
    }

    // [Issue#323]
    public void testCreatorPropRename() throws Exception
    {
        Bean323 input = new Bean323(7);
        assertEquals("{\"b\":7}", objectWriter().writeValueAsString(input));
    }

    // [Issue#327]
    public void testNonConflict() throws Exception
    {
        String json = objectMapper().writeValueAsString(new BogusConflictBean());
        assertEquals(aposToQuotes("{'prop1':2,'prop2':1}"), json);
    }    
}

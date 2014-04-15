package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests verifying handling of potential and actual
 * conflicts, regarding property handling.
 */
public class TestPropertyConflict323 extends BaseMapTest
{
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
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#323]
    public void testCreatorPropRename() throws Exception
    {
        Bean323 input = new Bean323(7);
        assertEquals("{\"b\":7}", objectWriter().writeValueAsString(input));
    }
}

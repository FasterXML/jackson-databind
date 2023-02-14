package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests verifying handling of potential and actual
 * conflicts, regarding property handling.
 */
public class TestPropertyRename extends BaseMapTest
{
    static class Bean323WithIgnore {
        @JsonIgnore
        private int a;

        public Bean323WithIgnore(@JsonProperty("a") final int a ) {
            this.a = a;
        }

        @JsonProperty("b")
        private int getA () {
            return a;
        }
    }

    @JsonPropertyOrder({ "a","b" })
    static class Bean323WithExplicitCleave1 {
        @JsonProperty("a")
        private int a;

        public Bean323WithExplicitCleave1(@JsonProperty("a") final int a ) {
            this.a = a;
        }

        @JsonProperty("b")
        private int getA () {
            return a;
        }
    }

    @JsonPropertyOrder({ "a","b" })
    static class Bean323WithExplicitCleave2 {
        @JsonProperty("b")
        private int a;

        public Bean323WithExplicitCleave2(@JsonProperty("a") final int a ) {
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

    public void testCreatorPropRenameWithIgnore() throws Exception
    {
        Bean323WithIgnore input = new Bean323WithIgnore(7);
        assertEquals("{\"b\":7}", objectWriter().writeValueAsString(input));
    }

    public void testCreatorPropRenameWithCleave() throws Exception
    {
        assertEquals("{\"a\":7,\"b\":7}",
        		objectWriter().writeValueAsString(new Bean323WithExplicitCleave1(7)));
        // note: 'a' NOT included as only ctor property found for it, no getter/field
        assertEquals("{\"b\":7}", objectWriter().writeValueAsString(new Bean323WithExplicitCleave2(7)));
    }
}

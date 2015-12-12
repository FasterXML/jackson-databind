package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.*;

// mostly for [databind#1033]
public class SetterConflictTest extends BaseMapTest
{
    // Should prefer primitives over Strings, more complex types, by default
    static class Issue1033Bean {
        public int value;

        public void setValue(int v) { value = v; }
        public void setValue(Issue1033Bean foo) {
            throw new Error("Should not get called");
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    private final ObjectMapper MAPPER = objectMapper();

    public void testSetterPriority() throws Exception
    {
        Issue1033Bean bean = MAPPER.readValue(aposToQuotes("{'value':42}"),
                Issue1033Bean.class);
        assertEquals(42, bean.value);
    }
}

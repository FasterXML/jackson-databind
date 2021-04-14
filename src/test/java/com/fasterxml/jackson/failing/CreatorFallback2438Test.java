package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

// for [databind#2438]
public class CreatorFallback2438Test extends BaseMapTest
{
    static class Creator2438 {
        int value;

        @JsonCreator
        public Creator2438(@JsonProperty("value") int v) {
//System.err.println("DEBUG: value set as "+v);
            value = v;
        }

        public int accessValue() { return value; }

        // This or visible (public) setter are required to show the issue
        public void setValue(int v) { value = v; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testCreator2438() throws Exception
    {
        // note: by default, duplicate-detection not enabled, so should not
        // throw exception. But should also NOT apply second value because
        // field/setter should NOT be used in case there is already creator property
        Creator2438 value = MAPPER.readValue(a2q( "{'value':1, 'value':2}"),
                Creator2438.class);
        assertEquals(1, value.accessValue());
    }
}

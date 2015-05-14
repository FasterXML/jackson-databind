package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

// tests for [databind#296]
public class TransientFieldTest extends BaseMapTest
{
    @JsonPropertyOrder({ "x" })
    static class ClassyTransient
    {
        public transient int value = 3;

        public int getValue() { return value; }

        public int getX() { return 42; }
    }

    public void testTransientFieldHandling() throws Exception
    {
        // default handling: remove transient field but do not propagate
        ObjectMapper m = objectMapper();
        assertEquals(aposToQuotes("{'x':42,'value':3}"),
                m.writeValueAsString(new ClassyTransient()));

        // but may change that
        m = new ObjectMapper()
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
        assertEquals(aposToQuotes("{'x':42}"),
                m.writeValueAsString(new ClassyTransient()));
    }
}

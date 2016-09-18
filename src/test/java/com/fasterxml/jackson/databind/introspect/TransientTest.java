package com.fasterxml.jackson.databind.introspect;

import java.beans.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;

/**
 * Tests for both `transient` keyword and JDK 7
 * {@link java.beans.Transient} annotation.
 */
public class TransientTest extends BaseMapTest
{
    // for [databind#296]
    @JsonPropertyOrder({ "x" })
    static class ClassyTransient
    {
        public transient int value = 3;

        public int getValue() { return value; }

        public int getX() { return 42; }
    }

    static class SimplePrunableTransient {
        public int a = 1;
        public transient int b = 2;
    }
    
    // for [databind#857]
    static class BeanTransient {
        @Transient
        public int getX() { return 3; }

        public int getY() { return 4; }
    }

    // for [databind#1184]
    static class OverridableTransient {
        @JsonProperty
//        @JsonProperty("value") // should override transient here, to force inclusion
        public transient int tValue;

        public OverridableTransient(int v) { tValue = v; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    // for [databind#296]
    public void testTransientFieldHandling() throws Exception
    {
        // default handling: remove transient field but do not propagate
        assertEquals(aposToQuotes("{'x':42,'value':3}"),
                MAPPER.writeValueAsString(new ClassyTransient()));
        assertEquals(aposToQuotes("{'a':1}"),
                MAPPER.writeValueAsString(new SimplePrunableTransient()));

        // but may change that
        ObjectMapper m = new ObjectMapper()
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
        assertEquals(aposToQuotes("{'x':42}"),
                m.writeValueAsString(new ClassyTransient()));
    }

    // for [databind#857]
    public void testBeanTransient() throws Exception
    {
        assertEquals(aposToQuotes("{'y':4}"),
                MAPPER.writeValueAsString(new BeanTransient()));
    }

    // for [databind#1184]
    public void testOverridingTransient() throws Exception
    {
        assertEquals(aposToQuotes("{'tValue':38}"),
                MAPPER.writeValueAsString(new OverridableTransient(38)));
    }
}

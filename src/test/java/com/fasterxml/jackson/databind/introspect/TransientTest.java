package com.fasterxml.jackson.databind.introspect;

import java.beans.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

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

    static class TransientToPrune {
        public transient int a;

        public int getA() { return a; }
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
        assertEquals(a2q("{'x':42,'value':3}"),
                MAPPER.writeValueAsString(new ClassyTransient()));
        assertEquals(a2q("{'a':1}"),
                MAPPER.writeValueAsString(new SimplePrunableTransient()));

        // but may change that
        ObjectMapper m = jsonMapperBuilder()
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
            .build();
        assertEquals(a2q("{'x':42}"),
                m.writeValueAsString(new ClassyTransient()));
    }

    // for [databind#857]
    public void testBeanTransient() throws Exception
    {
        assertEquals(a2q("{'y':4}"),
                MAPPER.writeValueAsString(new BeanTransient()));
    }

    // for [databind#1184]
    public void testOverridingTransient() throws Exception
    {
        assertEquals(a2q("{'tValue':38}"),
                MAPPER.writeValueAsString(new OverridableTransient(38)));
    }

    // for [databind#3682]: SHOULD prune `transient` Field, not pull in
    public void testTransientToPrune() throws Exception
    {
        try {
            TransientToPrune result = MAPPER.readValue("{\"a\":3}",
                    TransientToPrune.class);
            fail("Should not pass, got: "+result);
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized", "\"a\"");
        }
    }
}

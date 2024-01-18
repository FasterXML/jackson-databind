package com.fasterxml.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for checking that static methods are not recognized as accessors
 * for properties
 */
public class NoStaticsDeserTest
{
    static class Bean
    {
        int _x;

        public static void setX(int value) { throw new Error("Should NOT call static method"); }

        @JsonProperty("x") public void assignX(int x) { _x = x; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    @Test
    public void testSimpleIgnore() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // should not care about static setter...
        Bean result = m.readValue("{ \"x\":3}", Bean.class);
        assertEquals(3, result._x);
    }
}

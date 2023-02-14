package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.IgnoredPropertyException;

/**
 * This unit test suite that tests use of {@link JsonIgnore}
 * annotation with deserialization.
 */
public class IgnoreWithDeserTest
    extends BaseMapTest
{
    // Class for testing {@link JsonIgnore} annotations with setters
    final static class SizeClassIgnore
    {
        int _x = 0;
        int _y = 0;

        public void setX(int value) { _x = value; }
        @JsonIgnore public void setY(int value) { _y = value; }

        // Just igoring won't help a lot here; let's define a replacement
        // so that we won't get an exception for "unknown field"
        @JsonProperty("y") void foobar(int value) {
            ; // nop
        }
    }

    @JsonIgnoreProperties({ "z" })
    final static class NoYOrZ
    {
        public int x;

        @JsonIgnore
        public int y = 1;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testSimpleIgnore() throws Exception
    {
        SizeClassIgnore result = MAPPER.readValue("{ \"x\":1, \"y\" : 2 }",
             SizeClassIgnore.class);
        // x should be set, y not
        assertEquals(1, result._x);
        assertEquals(0, result._y);
    }

    public void testFailOnIgnore() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(NoYOrZ.class);

        // First, fine to get "x":
        NoYOrZ result = r.readValue(a2q("{'x':3}"));
        assertEquals(3, result.x);
        assertEquals(1, result.y);

        // but not 'y'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            result = r.readValue(a2q("{'x':3, 'y':4}"));
            fail("Should fail");
        } catch (IgnoredPropertyException e) {
            verifyException(e, "Ignored field");
        }

        // or 'z'
        try {
            result = r.readValue(a2q("{'z':2 }"));
            fail("Should fail");
        } catch (IgnoredPropertyException e) {
            verifyException(e, "Ignored field");
        }
    }
}

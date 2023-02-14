package com.fasterxml.jackson.databind.ser.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * This unit test suite tests use of {@link JsonIgnore} annotations
 * with  bean serialization; as well as {@link JsonIgnoreType}.
 */
public class TestSimpleSerializationIgnore
    extends BaseMapTest
{
    // Class for testing enabled {@link JsonIgnore} annotation
    final static class SizeClassEnabledIgnore
    {
        @JsonIgnore public int getY() { return 9; }

        // note: must be public to be seen
        public int getX() { return 1; }

        @JsonIgnore public int getY2() { return 1; }
        @JsonIgnore public int getY3() { return 2; }
    }

    // Class for testing disabled {@link JsonIgnore} annotation
    final static class SizeClassDisabledIgnore
    {
        // note: must be public to be seen
        public int getX() { return 3; }
        @JsonIgnore(false) public int getY() { return 4; }
    }

    static class BaseClassIgnore
    {
        @JsonProperty("x")
        @JsonIgnore
        public int x() { return 1; }

        public int getY() { return 2; }
    }

    static class SubClassNonIgnore
        extends BaseClassIgnore
    {
        // Annotations to disable ignorance, in sub-class; note that
        // we must still get "JsonProperty" fro super class
        @Override
        @JsonIgnore(false)
        public int x() { return 3; }
    }

    @JsonIgnoreType
    static class IgnoredType { }

    @JsonIgnoreType(false)
    static class NonIgnoredType
    {
        public int value = 13;

        public IgnoredType ignored = new IgnoredType();
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleIgnore() throws Exception
    {
        // Should see "x", not "y"
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassEnabledIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get("x"));
        assertNull(result.get("y"));
    }

    public void testDisabledIgnore() throws Exception
    {
        // Should see "x" and "y"
        Map<String,Object> result = writeAndMap(MAPPER, new SizeClassDisabledIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(4), result.get("y"));
    }

    /**
     * Test case to verify that ignore tag can also be disabled
     * via inheritance
     */
    public void testIgnoreOver() throws Exception
    {
        // should only see "y"
        Map<String,Object> result = writeAndMap(MAPPER, new BaseClassIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(2), result.get("y"));

        // Should see "x" and "y"
        result = writeAndMap(MAPPER, new SubClassNonIgnore());
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
        assertEquals(Integer.valueOf(2), result.get("y"));
    }

    public void testIgnoreType() throws Exception
    {
        assertEquals("{\"value\":13}", MAPPER.writeValueAsString(new NonIgnoredType()));
    }
}

package com.fasterxml.jackson.databind.filter;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Unit tests for checking that alternative settings for
 * {@link JsonSerialize#include} annotation property work
 * as expected.
 */
public class JsonIncludeTest
    extends BaseMapTest
{
    static class SimpleBean
    {
        public String getA() { return "a"; }
        public String getB() { return null; }
    }
    
    @JsonInclude(JsonInclude.Include.ALWAYS) // just to ensure default
    static class NoNullsBean
    {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getA() { return null; }

        public String getB() { return null; }
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class NonDefaultBean
    {
        String _a = "a", _b = "b";

        NonDefaultBean() { }

        public String getA() { return _a; }
        public String getB() { return _b; }
    }

    // [databind#998]: Do not require no-arg constructor; but if not, defaults check
    //    has weaker interpretation
    @JsonPropertyOrder({ "x", "y", "z" })
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class NonDefaultBeanXYZ
    {
        public int x;
        public int y = 3;
        public int z = 7;

        NonDefaultBeanXYZ(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    
    static class MixedBean
    {
        String _a = "a", _b = "b";

        MixedBean() { }

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public String getA() { return _a; }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getB() { return _b; }
    }

    // to ensure that default values work for collections as well
    static class ListBean {
        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public List<String> strings = new ArrayList<String>();
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class ArrayBean {
        public int[] ints = new int[] { 1, 2 };
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    final private ObjectMapper MAPPER = new ObjectMapper();

    public void testGlobal() throws IOException
    {
        Map<String,Object> result = writeAndMap(MAPPER, new SimpleBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertNull(result.get("b"));
        assertTrue(result.containsKey("b"));
    }

    public void testNonNullByClass() throws IOException
    {
        Map<String,Object> result = writeAndMap(MAPPER, new NoNullsBean());
        assertEquals(1, result.size());
        assertFalse(result.containsKey("a"));
        assertNull(result.get("a"));
        assertTrue(result.containsKey("b"));
        assertNull(result.get("b"));
    }

    public void testNonDefaultByClass() throws IOException
    {
        NonDefaultBean bean = new NonDefaultBean();
        // need to change one of defaults
        bean._a = "notA";
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("a"));
        assertEquals("notA", result.get("a"));
        assertFalse(result.containsKey("b"));
        assertNull(result.get("b"));
    }

    // [databind#998]
    public void testNonDefaultByClassNoCtor() throws IOException
    {
        NonDefaultBeanXYZ bean = new NonDefaultBeanXYZ(1, 2, 0);
        String json = MAPPER.writeValueAsString(bean);
        assertEquals(aposToQuotes("{'x':1,'y':2}"), json);
    }
    
    public void testMixedMethod() throws IOException
    {
        MixedBean bean = new MixedBean();
        bean._a = "xyz";
        bean._b = null;
        Map<String,Object> result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals("xyz", result.get("a"));
        assertFalse(result.containsKey("b"));

        bean._a = "a";
        bean._b = "b";
        result = writeAndMap(MAPPER, bean);
        assertEquals(1, result.size());
        assertEquals("b", result.get("b"));
        assertFalse(result.containsKey("a"));
    }

    public void testDefaultForEmptyList() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ListBean()));
    }

    // NON_DEFAULT shoud work for arrays too
    public void testNonEmptyDefaultArray() throws IOException
    {
        assertEquals("{}", MAPPER.writeValueAsString(new ArrayBean()));
    }
}

package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestIgnoreProperties
    extends BaseMapTest
{
    /*
     ****************************************************************
     * Helper classes
     ****************************************************************
     */

    @JsonIgnoreProperties({"b", "c"})
    static class IgnoreSome
    {
        public int a = 3;
        public String b = "x";

        public int getC() { return -6; }
        public String getD() { return "abc"; }
    }

    @SuppressWarnings("serial")
    @JsonIgnoreProperties({"@class"})
    static class MyMap extends HashMap<String,String> { }
    
    /*
     ****************************************************************
     * Unit tests
     ****************************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testExplicitIgnoralWithBean() throws Exception
    {
        IgnoreSome value = new IgnoreSome();
        Map<String,Object> result = writeAndMap(MAPPER, value);
        assertEquals(2, result.size());
        // verify that specified fields are ignored
        assertFalse(result.containsKey("b"));
        assertFalse(result.containsKey("c"));
        // and that others are not
        assertEquals(Integer.valueOf(value.a), result.get("a"));
        assertEquals(value.getD(), result.get("d"));
    }

    public void testExplicitIgnoralWithMap() throws Exception
    {
        // test simulating need to filter out metadata like class name
        MyMap value = new MyMap();
        value.put("a", "b");
        value.put("@class", MyMap.class.getName());
        Map<String,Object> result = writeAndMap(MAPPER, value);
        assertEquals(1, result.size());
        // verify that specified field is ignored
        assertFalse(result.containsKey("@class"));
        // and that others are not
        assertEquals(value.get("a"), result.get("a"));
    }
}

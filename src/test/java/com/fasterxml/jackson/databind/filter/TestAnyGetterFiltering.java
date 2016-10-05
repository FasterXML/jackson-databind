package com.fasterxml.jackson.databind.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Unit tests for ensuring that entries accessible via "any filter"
 * can also be filtered with JSON Filter functionality.
 */
public class TestAnyGetterFiltering extends BaseMapTest
{
    @JsonFilter("anyFilter")
    public static class AnyBean
    {
        private Map<String, String> properties = new HashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("b", "2");
        }

        @JsonAnyGetter
        public Map<String, String> anyProperties()
        {
            return properties;
        }
    }

    public static class AnyBeanWithIgnores
    {
        private Map<String, String> properties = new LinkedHashMap<String, String>();
        {
            properties.put("a", "1");
            properties.put("bogus", "2");
            properties.put("b", "3");
        }

        @JsonAnyGetter
        @JsonIgnoreProperties({ "bogus" })
        public Map<String, String> anyProperties()
        {
            return properties;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testAnyGetterFiltering() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"2\"}", mapper.writer(prov).writeValueAsString(new AnyBean()));
    }

    // for [databind#1142]
    public void testAnyGetterIgnore() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(aposToQuotes("{'a':'1','b':'3'}"),
                mapper.writeValueAsString(new AnyBeanWithIgnores()));
    }
}

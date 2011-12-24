package com.fasterxml.jackson.failing;

import java.util.HashMap;
import java.util.Map;


import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Currently (1.8) generic filtering does not work for "any getter": it should,
 * ideally, so here's the test.
 */
public class TestSerializationFiltering extends BaseMapTest
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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // should also work for @JsonAnyGetter, as per [JACKSON-516]
    public void testAnyGetterFiltering() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"a\":\"1\"}", mapper.writer(prov).writeValueAsString(new AnyBean()));
    }
}

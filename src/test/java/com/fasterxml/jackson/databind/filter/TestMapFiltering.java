package com.fasterxml.jackson.databind.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFilter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class TestMapFiltering extends BaseMapTest
{
    static class MapBean {
        @JsonFilter("filterX")
        public Map<String,Integer> values;
        
        public MapBean() {
            values = new LinkedHashMap<String,Integer>();
            values.put("a", 1);
            values.put("b", 5);
            values.put("c", 9);
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
        FilterProvider prov = new SimpleFilterProvider().addFilter("filterX",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals(aposToQuotes("{'values':{'b':5}}"),
                mapper.writer(prov).writeValueAsString(new MapBean()));
    }

}

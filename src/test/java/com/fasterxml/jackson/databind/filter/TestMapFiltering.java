package com.fasterxml.jackson.databind.filter;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class TestMapFiltering extends BaseMapTest
{
    @SuppressWarnings("serial")
    @JsonFilter("filterForMaps")
    static class FilteredBean extends LinkedHashMap<String,Integer> { }
    
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

    final ObjectMapper MAPPER = objectMapper();
    
    public void testMapFilteringViaProps() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("filterX",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals(aposToQuotes("{'values':{'b':5}}"),
                MAPPER.writer(prov).writeValueAsString(new MapBean()));
    }

    public void testMapFilteringViaClass() throws Exception
    {
        FilteredBean bean = new FilteredBean();
        bean.put("a", 4);
        bean.put("b", 3);
        FilterProvider prov = new SimpleFilterProvider().addFilter("filterForMaps",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals(aposToQuotes("{'b':3}"),
                MAPPER.writer(prov).writeValueAsString(bean));
    }
    
}

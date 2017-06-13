package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Unit tests for ensuring that entries accessible via "any filter"
 * can also be filtered with JSON Filter functionality.
 */
public class TestAnyGetter1655Filtering extends BaseMapTest
{
    // [databind#1655]
    @JsonFilter("CustomFilter")
    static class OuterObject {
         public int getExplicitProperty() {
              return 42;
         }

         @JsonAnyGetter
         public Map<String, Object> getAny() {
              Map<String, Object> extra = new HashMap<>();
              extra.put("dynamicProperty", "I will not serialize");
              return extra;
         }
    }

    static class CustomFilter extends SimpleBeanPropertyFilter {
         @Override
         public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
             if (pojo instanceof OuterObject) {
                   writer.serializeAsField(pojo, jgen, provider);
              }
         }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1655]
    public void testAnyGetterPojo1655() throws Exception
    {
        FilterProvider filters = new SimpleFilterProvider().addFilter("CustomFilter", new CustomFilter());
        String json = MAPPER.writer(filters).writeValueAsString(new OuterObject());
        Map<?,?> stuff = MAPPER.readValue(json, Map.class);
        if (stuff.size() != 2) {
            fail("Should have 2 properties, got: "+stuff);
        }
   }
}

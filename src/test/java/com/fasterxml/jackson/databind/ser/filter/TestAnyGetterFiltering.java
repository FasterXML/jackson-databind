package com.fasterxml.jackson.databind.ser.filter;

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
         public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider provider,
                 PropertyWriter writer) throws Exception
         {
             if (pojo instanceof OuterObject) {
                 writer.serializeAsField(pojo, gen, provider);
              }
         }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testAnyGetterFiltering() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("anyFilter",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        assertEquals("{\"b\":\"2\"}", MAPPER.writer(prov).writeValueAsString(new AnyBean()));
    }

    // for [databind#1142]
    public void testAnyGetterIgnore() throws Exception
    {
        assertEquals(a2q("{'a':'1','b':'3'}"),
                MAPPER.writeValueAsString(new AnyBeanWithIgnores()));
    }

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

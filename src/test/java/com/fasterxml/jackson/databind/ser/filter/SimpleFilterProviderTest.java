package com.fasterxml.jackson.databind.ser.filter;

import java.util.Map;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonFilter;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * Tests {@link SimpleFilterProvider} on registration of filters.
 */
public class SimpleFilterProviderTest extends BaseMapTest
{
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @JsonFilter("filterB")
    public static class AnyBeanB
    {
        public String a;
        public String b;

        public AnyBeanB(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }

    @JsonFilter(value = "")
    public static class AnyBeanC
    {
        public String c;
        public String d;

        public AnyBeanC(String c, String d) {
            this.c = c;
            this.d = d;
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testAddFilterLastOneRemains() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("filterB", SimpleBeanPropertyFilter.filterOutAllExcept());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);

        assertEquals(a2q("{}"), jsonString);
    }

    public void testAddFilterLastOneRemainsFlip() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
                .addFilter("filterB", SimpleBeanPropertyFilter.serializeAll());
        AnyBeanB beanB = new AnyBeanB("1a", "2b");

        String jsonString = MAPPER.writer(prov).writeValueAsString(beanB);
        
        Map<String, Object> actualMap = MAPPER.readValue(jsonString, Map.class);
        Map<String, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put("a", "1a");
        expectedMap.put("b", "2b");

        assertEquals(expectedMap, actualMap);
    }

    public void testAddFilterWithEmptyStringId() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("", SimpleBeanPropertyFilter.filterOutAllExcept("d"));
        AnyBeanC bean = new AnyBeanC(null, "D is filtered");

        String jsonString = MAPPER.writer(prov).writeValueAsString(bean);

        assertEquals(a2q("{'c':null,'d':'D is filtered'}"), jsonString);
    }

    public void testAddingNullFilter2ThrowsException() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter("filterB", null);
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanB beanD = new AnyBeanB("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "No filter configured with id 'filterB'");
        }
    }

    public void testAddingNullFilterIdThrowsException() throws Exception {
        FilterProvider prov = new SimpleFilterProvider()
                .addFilter(null, SimpleBeanPropertyFilter.serializeAll());
        ObjectWriter writer = MAPPER.writer(prov);
        AnyBeanB beanD = new AnyBeanB("1a", "2b");

        try {
            writer.writeValueAsString(beanD);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "No filter configured with id 'filterB'");
        }
    }
}

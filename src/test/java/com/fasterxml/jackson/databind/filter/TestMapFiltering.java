package com.fasterxml.jackson.databind.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class TestMapFiltering extends BaseMapTest
{
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomOffset
    {
        public int value();
    }

    @SuppressWarnings("serial")
    @JsonFilter("filterForMaps")
    static class FilteredBean extends LinkedHashMap<String,Integer> { }
    
    static class MapBean {
        @JsonFilter("filterX")
        @CustomOffset(1)
        public Map<String,Integer> values;
        
        public MapBean() {
            values = new LinkedHashMap<String,Integer>();
            values.put("a", 1);
            values.put("b", 5);
            values.put("c", 9);
        }
    }

    static class MyMapFilter implements PropertyFilter
    {
        @Override
        public void serializeAsField(Object value, JsonGenerator jgen,
                SerializerProvider provider, PropertyWriter writer)
            throws Exception
        {
            String name = writer.getName();
            if (!"a".equals(name)) {
                return;
            }
            CustomOffset n = writer.findAnnotation(CustomOffset.class);
            int offset = (n == null) ? 0 : n.value();
            Integer I = offset + ((Integer) value).intValue();

            writer.serializeAsField(I, jgen, provider);
        }

        @Override
        public void serializeAsElement(Object elementValue, JsonGenerator jgen,
                SerializerProvider prov, PropertyWriter writer)
                throws Exception {
            // not needed for testing
        }

        @Override
        public void depositSchemaProperty(PropertyWriter writer,
                ObjectNode propertiesNode, SerializerProvider provider)
                throws JsonMappingException {
            
        }

        @Override
        public void depositSchemaProperty(PropertyWriter writer,
                JsonObjectFormatVisitor objectVisitor,
                SerializerProvider provider) throws JsonMappingException {
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
        String json = MAPPER.writer(prov).writeValueAsString(new MapBean());
        assertEquals(aposToQuotes("{'values':{'b':5}}"), json);
    }

    public void testMapFilteringViaClass() throws Exception
    {
        FilteredBean bean = new FilteredBean();
        bean.put("a", 4);
        bean.put("b", 3);
        FilterProvider prov = new SimpleFilterProvider().addFilter("filterForMaps",
                SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        String json = MAPPER.writer(prov).writeValueAsString(bean);
        assertEquals(aposToQuotes("{'b':3}"), json);
    }
    
    // [Issue#522]
    public void testMapFilteringWithAnnotations() throws Exception
    {
        FilterProvider prov = new SimpleFilterProvider().addFilter("filterX",
                new MyMapFilter());
        String json = MAPPER.writer(prov).writeValueAsString(new MapBean());
        // a=1 should become a=2
        assertEquals(aposToQuotes("{'values':{'a':2}}"), json);
    }
}

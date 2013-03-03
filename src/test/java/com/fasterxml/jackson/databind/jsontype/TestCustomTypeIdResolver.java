package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class TestCustomTypeIdResolver extends BaseMapTest
{
    @JsonTypeInfo(use=Id.CUSTOM, include=As.WRAPPER_OBJECT)
    @JsonTypeIdResolver(CustomResolver.class)
    static class CustomBean {
        public int x;
        
        public CustomBean() { }
        public CustomBean(int x) { this.x = x; }
    }
    
    static class CustomResolver implements TypeIdResolver
    {
        static List<JavaType> initTypes;

        public CustomResolver() { }
        
        @Override
        public Id getMechanism() {
            return Id.CUSTOM;
        }

        @Override
        public String idFromValue(Object value)
        {
            if (value.getClass() == CustomBean.class) {
                return "*";
            }
            return "unknown";
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return idFromValue(value);
        }

        @Override
        public void init(JavaType baseType) {
            if (initTypes != null) {
                initTypes.add(baseType);
            }
        }

        @Override
        public JavaType typeFromId(String id)
        {
            if ("*".equals(id)) {
                return TypeFactory.defaultInstance().constructType(CustomBean.class);
            }
            return null;
        }

        @Override
        public String idFromBaseType() {
            return "xxx";
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // for [JACKSON-359]
    public void testCustomTypeIdResolver() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        List<JavaType> types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        String json = m.writeValueAsString(new CustomBean[] { new CustomBean(28) });
        assertEquals("[{\"*\":{\"x\":28}}]", json);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());

        types = new ArrayList<JavaType>();
        CustomResolver.initTypes = types;
        CustomBean[] result = m.readValue(json, CustomBean[].class);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(28, result[0].x);
        assertEquals(1, types.size());
        assertEquals(CustomBean.class, types.get(0).getRawClass());
    }
}

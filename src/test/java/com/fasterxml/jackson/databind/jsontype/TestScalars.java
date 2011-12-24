package com.fasterxml.jackson.databind.jsontype;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestScalars extends BaseMapTest
{
    private static class DynamicWrapper {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
        public Object value;
        
        @SuppressWarnings("unused")
        public DynamicWrapper() { }
        public DynamicWrapper(Object v) { value = v; }
    }

    static enum TestEnum { A, B; }

    private static class AbstractWrapper {
        @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
        public Serializable value;
        
        @SuppressWarnings("unused")
        public AbstractWrapper() { }
        public AbstractWrapper(Serializable v) { value = v; }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * Ensure that per-property dynamic types work, both for "native" types
     * and others
     */
    public void testScalarsWithTyping() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json;
        DynamicWrapper result;

        // first, check "native" types
        json = m.writeValueAsString(new DynamicWrapper(Integer.valueOf(3)));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals(Integer.valueOf(3), result.value);

        json = m.writeValueAsString(new DynamicWrapper("abc"));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals("abc", result.value);

        json = m.writeValueAsString(new DynamicWrapper("abc"));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals("abc", result.value);

        json = m.writeValueAsString(new DynamicWrapper(Boolean.TRUE));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals(Boolean.TRUE, result.value);
        
        // then verify other scalars
        json = m.writeValueAsString(new DynamicWrapper(Long.valueOf(7L)));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals(Long.valueOf(7), result.value);

        json = m.writeValueAsString(new DynamicWrapper(TestEnum.B));
        result = m.readValue(json, DynamicWrapper.class);
        assertEquals(TestEnum.B, result.value);
    }

    public void testScalarsViaAbstractType() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json;
        AbstractWrapper result;

        // first, check "native" types
        json = m.writeValueAsString(new AbstractWrapper(Integer.valueOf(3)));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals(Integer.valueOf(3), result.value);

        json = m.writeValueAsString(new AbstractWrapper("abc"));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals("abc", result.value);

        json = m.writeValueAsString(new AbstractWrapper("abc"));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals("abc", result.value);

        json = m.writeValueAsString(new AbstractWrapper(Boolean.TRUE));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals(Boolean.TRUE, result.value);
        
        // then verify other scalars
        json = m.writeValueAsString(new AbstractWrapper(Long.valueOf(7L)));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals(Long.valueOf(7), result.value);

        json = m.writeValueAsString(new AbstractWrapper(TestEnum.B));
        result = m.readValue(json, AbstractWrapper.class);
        assertEquals(TestEnum.B, result.value);
    }
}

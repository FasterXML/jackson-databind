package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestVisibleExternalId extends BaseMapTest
{
    // [Issue#408]
    static class ExternalBeanWithId
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="type", visible=true)
        public ValueBean bean;

        public ExternalBeanWithId() { }
        public ExternalBeanWithId(int v) {
            bean = new ValueBean(v);
        }
    }

    @JsonTypeName("vbean")
    static class ValueBean {
        public int value;
        
        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    private final ObjectMapper MAPPER = objectMapper();
    
    // [Issue#408]
    public void testVisibleTypeId() throws Exception
    {
        String json = MAPPER.writeValueAsString(new ExternalBeanWithId(3));
        ExternalBeanWithId result = MAPPER.readValue(json, ExternalBeanWithId.class);
        assertNotNull(result);
        assertNotNull(result.bean);
        assertEquals(3, result.bean.value);
    }
}

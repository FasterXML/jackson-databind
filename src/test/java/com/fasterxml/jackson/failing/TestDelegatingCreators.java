package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JacksonInject;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDelegatingCreators extends BaseMapTest
{
    // for [JACKSON-711]; should allow delegate-based one(s) too
    static class CtorBean711
    {
        protected String name;
        protected int age;
        
        @JsonCreator
        public CtorBean711(@JacksonInject String n, int a)
        {
            name = n;
            age = a;
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    // As per [JACKSON-711]: should also work with delegate model (single non-annotated arg)
    public void testWithCtorAndDelegate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Pooka")
            );
        CtorBean711 bean = null;
        try {
            bean = mapper.readValue("38", CtorBean711.class);
        } catch (JsonMappingException e) {
            fail("Did not expect problems, got: "+e.getMessage());
        }
        assertEquals(38, bean.age);
        assertEquals("Pooka", bean.name);
    }
}

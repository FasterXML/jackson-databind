package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JacksonInject;

import com.fasterxml.jackson.databind.*;

public class TestCreatorsDelegating extends BaseMapTest
{
    static class BooleanBean
    {
        protected Boolean value;

        public BooleanBean(Boolean v) { value = v; }
        
        @JsonCreator
        protected static BooleanBean create(Boolean value) {
            return new BooleanBean(value);
        }
    }

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

    // for [JACKSON-711]; should allow delegate-based one(s) too
    static class FactoryBean711
    {
        protected String name1;
        protected String name2;
        protected int age;
        
        private FactoryBean711(int a, String n1, String n2) {
            age = a;
            name1 = n1;
            name2 = n2;
        }
        
        @JsonCreator
        public static FactoryBean711 create(@JacksonInject String n1, int a, @JacksonInject String n2) {
            return new FactoryBean711(a, n1, n2);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testBooleanDelegate() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // should obviously work with booleans...
        BooleanBean bb = m.readValue("true", BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);

        // but also with value conversion from String
        bb = m.readValue(quote("true"), BooleanBean.class);
        assertEquals(Boolean.TRUE, bb.value);
    }
    
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

    public void testWithFactoryAndDelegate() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Fygar")
            );
        FactoryBean711 bean = null;
        try {
            bean = mapper.readValue("38", FactoryBean711.class);
        } catch (JsonMappingException e) {
            fail("Did not expect problems, got: "+e.getMessage());
        }
        assertEquals(38, bean.age);
        assertEquals("Fygar", bean.name1);
        assertEquals("Fygar", bean.name2);
    }
}

package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class TestInjectables extends BaseMapTest
{
    static class InjectedBean
    {
        @JacksonInject
        protected String stuff;

        @JacksonInject("myId")
        protected String otherStuff;

        protected long third;
        
        public int value;

        @JacksonInject
        public void injectThird(long v) {
            third = v;
        }
    }    

    static class BadBean1 {
        @JacksonInject protected String prop1;
        @JacksonInject protected String prop2;
    }

    static class BadBean2 {
        @JacksonInject("x") protected String prop1;
        @JacksonInject("x") protected String prop2;
    }

    static class CtorBean {
        protected String name;
        protected int age;
        
        public CtorBean(@JacksonInject String n, @JsonProperty("age") int a)
        {
            name = n;
            age = a;
        }
    }

    static class CtorBean2 {
        protected String name;
        protected Integer age;
        
        public CtorBean2(@JacksonInject String n, @JacksonInject("number") Integer a)
        {
            name = n;
            age = a;
        }
    }

    static class IssueGH471Bean {

        private final Object constructorInjected;
        private final String constructorValue;

        @JacksonInject("field_injected") private Object fieldInjected;
        @JsonProperty("field_value")     private String fieldValue;

        private Object methodInjected;
        private String methodValue;

        public int x;
        
        @JsonCreator
        private IssueGH471Bean(@JacksonInject("constructor_injected") Object constructorInjected,
                               @JsonProperty("constructor_value") String constructorValue) {
            this.constructorInjected = constructorInjected;
            this.constructorValue = constructorValue;
        }

        @JacksonInject("method_injected")
        private void setMethodInjected(Object methodInjected) {
            this.methodInjected = methodInjected;
        }

        @JsonProperty("method_value")
        public void setMethodValue(String methodValue) {
            this.methodValue = methodValue;
        }

    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "stuffValue")
            .addValue("myId", "xyz")
            .addValue(Long.TYPE, Long.valueOf(37))
            );
        InjectedBean bean = mapper.readValue("{\"value\":3}", InjectedBean.class);
        assertEquals(3, bean.value);
        assertEquals("stuffValue", bean.stuff);
        assertEquals("xyz", bean.otherStuff);
        assertEquals(37L, bean.third);
    }
    
    public void testWithCtors() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Bubba")
            );
        CtorBean bean = mapper.readValue("{\"age\":55}", CtorBean.class);
        assertEquals(55, bean.age);
        assertEquals("Bubba", bean.name);
    }

    // [Issue-13]
    public void testTwoInjectablesViaCreator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "Bob")
            .addValue("number", Integer.valueOf(13))
            );
        CtorBean2 bean = mapper.readValue("{ }", CtorBean2.class);
        assertEquals(Integer.valueOf(13), bean.age);
        assertEquals("Bob", bean.name);
    }
    
    public void testInvalidDup() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue("{}", BadBean1.class);
        } catch (Exception e) {
            verifyException(e, "Duplicate injectable value");
        }
        try {
            mapper.readValue("{}", BadBean2.class);
        } catch (Exception e) {
            verifyException(e, "Duplicate injectable value");
        }
    }

    public void testIssueGH471() throws Exception
    {
        final Object constructorInjected = "constructorInjected";
        final Object methodInjected = "methodInjected";
        final Object fieldInjected = "fieldInjected";

        ObjectMapper mapper = new ObjectMapper()
                        .setInjectableValues(new InjectableValues.Std()
                                .addValue("constructor_injected", constructorInjected)
                                .addValue("method_injected", methodInjected)
                                .addValue("field_injected", fieldInjected));

        IssueGH471Bean bean = mapper.readValue("{\"x\":13,\"constructor_value\":\"constructor\",\"method_value\":\"method\",\"field_value\":\"field\"}",
                IssueGH471Bean.class);

        /* Assert *SAME* instance */
        assertSame(constructorInjected, bean.constructorInjected);
        assertSame(methodInjected, bean.methodInjected);
        assertSame(fieldInjected, bean.fieldInjected);

        /* Check that basic properties still work (better safe than sorry) */
        assertEquals("constructor", bean.constructorValue);
        assertEquals("method", bean.methodValue);
        assertEquals("field", bean.fieldValue);

        assertEquals(13, bean.x);
    }
}

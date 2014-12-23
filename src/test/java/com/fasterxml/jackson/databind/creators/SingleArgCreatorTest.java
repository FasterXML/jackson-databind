package com.fasterxml.jackson.databind.creators;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class SingleArgCreatorTest extends BaseMapTest
{
    // [databind#430]: single arg BUT named; should not delegate

    static class SingleNamedStringBean {
        final String _ss;

        @JsonCreator
        public SingleNamedStringBean(@JsonProperty("") String ss){
            this._ss = ss;
        }

        public String getSs() { return _ss; }
    }

    // For [databind#614]
    static class SingleNamedButStillDelegating {
        protected final String value;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public SingleNamedButStillDelegating(@JsonProperty("foobar") String v){
            value = v;
        }

        public String getFoobar() { return "x"; }
    }
    
    // [databind#557]
    
    static class StringyBean
    {
        public final String value;

        private StringyBean(String value) { this.value = value; }

        public String getValue() {
            return value;
        }
    }

    static class StringyBeanWithProps
    {
        public final String value;

        @JsonCreator
        private StringyBeanWithProps(String v) { value = v; }

        public String getValue() {
            return value;
        }
    }

    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        private final String name;
        
        public MyParamIntrospector(String n) { name = n; }
        
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return name;
                }
                return "param"+ap.getIndex();
            }
            return super.findImplicitPropertyName(param);
        }
    }

    // [databind#660]
    static class ExplicitFactoryBean1 {
        private String value;

        private ExplicitFactoryBean1(String str) {
            value = null;
        }

        private ExplicitFactoryBean1(String str, boolean b) {
            value = str;
        }

        @JsonCreator
        public ExplicitFactoryBean1 create(String str) {
            ExplicitFactoryBean1 bean = new ExplicitFactoryBean1(str, false);
            bean.value = str;
            return bean;
        }

        public String value() { return value; }
    }

    // [databind#660]
    static class ExplicitFactoryBean2 {
        private String value;

        @JsonCreator
        private ExplicitFactoryBean2(String str) {
            value = null;
        }

        public static ExplicitFactoryBean2 valueOf(String str) {
            return new ExplicitFactoryBean2(null);
        }

        public String value() { return value; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

    public void testNamedSingleArg() throws Exception
    {
        SingleNamedStringBean bean = MAPPER.readValue(quote("foobar"),
                SingleNamedStringBean.class);
        assertEquals("foobar", bean._ss);
    }

    public void testSingleStringArgWithImplicitName() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector("value"));
        StringyBean bean = mapper.readValue(quote("foobar"), StringyBean.class);
        assertEquals("foobar", bean.getValue());
    }    

    // [databind#714]
    public void testSingleImplicitlyNamedNotDelegating() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector("value"));
        StringyBeanWithProps bean = mapper.readValue("{\"value\":\"x\"}", StringyBeanWithProps.class);
        assertEquals("x", bean.getValue());
    }    
    
    // [databind#714]
    public void testSingleExplicitlyNamedButDelegating() throws Exception
    {
        SingleNamedButStillDelegating bean = MAPPER.readValue(quote("xyz"),
                SingleNamedButStillDelegating.class);
        assertEquals("xyz", bean.value);
    }

    public void testExplicitFactory660() throws Exception
    {
        // First, explicit override for factory
        ExplicitFactoryBean1 bean = MAPPER.readValue(quote("abc"), ExplicitFactoryBean1.class);
        assertNotNull(bean);
        assertEquals("abc", bean.value());

        // and then one for private constructor
        ExplicitFactoryBean2 bean2 = MAPPER.readValue(quote("def"), ExplicitFactoryBean2.class);
        assertNotNull(bean2);
        assertEquals("def", bean2.value());
    }
}


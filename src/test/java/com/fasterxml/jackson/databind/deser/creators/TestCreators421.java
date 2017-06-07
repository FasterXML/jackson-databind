package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class TestCreators421 extends BaseMapTest
{
    static class MultiCtor
    {
        protected String _a, _b;
        
        private MultiCtor() { }
        private MultiCtor(String a, String b, Boolean c) {
            if (c == null) {
                throw new RuntimeException("Wrong factory!");
            }
            _a = a;
            _b = b;
        }

        @JsonCreator
        static MultiCtor factory(@JsonProperty("a") String a, @JsonProperty("b") String b) {
            return new MultiCtor(a, b, Boolean.TRUE);
        }
    }

    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return "a";
                case 1: return "b";
                case 2: return "c";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(param);
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#421]
    public void testMultiCtor421() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());

        MultiCtor bean = mapper.readValue(aposToQuotes("{'a':'123','b':'foo'}"), MultiCtor.class);
        assertNotNull(bean);
        assertEquals("123", bean._a);
        assertEquals("foo", bean._b);
    }
}

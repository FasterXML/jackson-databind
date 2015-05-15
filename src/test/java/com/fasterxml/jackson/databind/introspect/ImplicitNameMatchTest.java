package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ImplicitNameMatchTest extends BaseMapTest
{
    // Simple introspector that gives generated "ctorN" names for constructor
    // parameters
    static class ConstructorNameAI extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(AnnotatedMember member) {
            if (member instanceof AnnotatedParameter) {
                return String.format("ctor%d", ((AnnotatedParameter) member).getIndex());
            }
            return super.findImplicitPropertyName(member);
        }
    }
    
    @JsonPropertyOrder({ "first" ,"second", "other" })
    static class Issue792Bean
    {
        String value;

        public Issue792Bean(@JsonProperty("first") String a,
                @JsonProperty("second") String b) {
            value = a;
            // ignore second arg
        }

        public String getCtor0() { return value; }
        
        public int getOther() { return 3; }
    }

    static class Bean2
    {
        int x = 3;
        
        @JsonProperty("stuff")
        private void setValue(int i) { x = i; }

        public int getValue() { return x; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testBindingOfImplicitCreatorNames() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.setAnnotationIntrospector(new ConstructorNameAI());
        String json = m.writeValueAsString(new Issue792Bean("a", "b"));
        assertEquals(aposToQuotes("{'first':'a','other':3}"), json);
    }

    public void testImplicitWithSetterGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(new Bean2());
        assertEquals(aposToQuotes("{'stuff':3}"), json);
    }
}

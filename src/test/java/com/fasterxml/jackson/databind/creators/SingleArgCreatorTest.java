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

    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return "value";
                }
                return "param"+ap.getIndex();
            }
            return super.findImplicitPropertyName(param);
        }
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
        mapper.setAnnotationIntrospector(new MyParamIntrospector());
        StringyBean bean = mapper.readValue(quote("foobar"), StringyBean.class);
        assertEquals("foobar", bean.getValue());
    }    

    // [databind#714]
    public void testSingleExplicitlyNamedButDelegating() throws Exception
    {
        SingleNamedButStillDelegating bean = MAPPER.readValue(quote("xyz"),
                SingleNamedButStillDelegating.class);
        assertEquals("xyz", bean.value);
    }
}


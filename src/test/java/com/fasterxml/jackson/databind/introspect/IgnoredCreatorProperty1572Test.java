package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IgnoredCreatorProperty1572Test extends BaseMapTest
{
    static class InnerTest
    {
        public String str;
        public String otherStr;
    }

    static class OuterTest
    {
        InnerTest innerTest;

        @JsonIgnore
        String otherOtherStr;

        @JsonCreator
        public OuterTest(/*@JsonProperty("innerTest")*/ InnerTest inner,
                /*@JsonProperty("otherOtherStr")*/ String otherStr) {
            this.innerTest = inner;
        }
    }

    static class ImplicitNames extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(AnnotatedMember member) {
            if (member instanceof AnnotatedParameter) {
                // A placeholder for legitimate property name detection
                // such as what the JDK8 module provides
                AnnotatedParameter param = (AnnotatedParameter) member;
                switch (param.getIndex()) {
                case 0:
                    return "innerTest";
                case 1:
                    return "otherOtherStr";
                default:
                }
            }
            return null;
        }
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    // [databind#1572]
    public void testIgnoredCtorParam() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new ImplicitNames());
        String JSON = a2q("{'innerTest': {\n"
                +"'str':'str',\n"
                +"'otherStr': 'otherStr'\n"
                +"}}\n");
        OuterTest result = mapper.readValue(JSON, OuterTest.class);
        assertNotNull(result);
        assertNotNull(result.innerTest);
        assertEquals("otherStr", result.innerTest.otherStr);
    }
}

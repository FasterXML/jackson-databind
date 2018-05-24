package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class CreatorWithNamingStrategy2008Test extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                return "paramName"+ap.getIndex();
            }
            return super.findImplicitPropertyName(param);
        }
    }

    // wrt [https://github.com/FasterXML/jackson-modules-java8/issues/67]
    static class OneProperty {
        public String paramName0;

        @JsonCreator
        public OneProperty(String bogus) {
            paramName0 = "CTOR:"+bogus;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper()
            .setAnnotationIntrospector(new MyParamIntrospector())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            ;

    // Possibly [databind#2008], but originally
    // wrt [https://github.com/FasterXML/jackson-modules-java8/issues/67]
    public void testSnakeCaseWithOneArg() throws Exception
    {
        final String MSG = "1st";
        OneProperty actual = MAPPER.readValue(
                "{\"first_property\":\""+MSG+"\"}",
                OneProperty.class);
        assertEquals("CTOR:"+MSG, actual.paramName0);
    }
}

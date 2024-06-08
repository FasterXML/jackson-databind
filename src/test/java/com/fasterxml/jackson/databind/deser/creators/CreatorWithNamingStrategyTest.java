package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class CreatorWithNamingStrategyTest
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

    // [databind#2051]
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

    private final ObjectMapper MAPPER = newJsonMapper()
            .setAnnotationIntrospector(new MyParamIntrospector())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            ;

    // [databind#2051]
    @Test
    public void testSnakeCaseWithOneArg() throws Exception
    {
        final String MSG = "1st";
        OneProperty actual = MAPPER.readValue(
                "{\"param_name0\":\""+MSG+"\"}",
                OneProperty.class);
        assertEquals("CTOR:"+MSG, actual.paramName0);
    }
}

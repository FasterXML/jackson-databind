package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ImplicitParamsForCreator806Test extends BaseMapTest
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

    static class XY {
        protected int x, y;

        // annotation should NOT be needed with 2.6 any more (except for single-arg case)
//        @com.fasterxml.jackson.annotation.JsonCreator
        public XY(int x, int y) {
            this.x = x;
            this.y = y;
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

    // for [databind#806]: problem is that renaming occurs too late for implicitly detected
    // Creators
    public void testImplicitNameWithNamingStrategy() throws Exception
    {
        XY value = MAPPER.readValue(a2q("{'param_name0':1,'param_name1':2}"), XY.class);
        assertNotNull(value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }
}

package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImplicitParamsForCreator806Test extends DatabindTestUtil {
    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                return "paramName" + ap.getIndex();
            }
            return super.findImplicitPropertyName(param);
        }
    }

    static class XY806 {
        protected int x, y;

        // annotation should NOT be needed with 2.6 any more (except for single-arg case)
//        @com.fasterxml.jackson.annotation.JsonCreator
        public XY806(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper()
            .setAnnotationIntrospector(new MyParamIntrospector())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    // for [databind#806]: problem is that renaming occurs too late for implicitly detected
    // Creators
    @Test
    void implicitNameWithNamingStrategy() throws Exception {
        XY806 value = MAPPER.readValue(a2q("{'param_name0':1,'param_name1':2}"), XY806.class);
        assertNotNull(value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }
}

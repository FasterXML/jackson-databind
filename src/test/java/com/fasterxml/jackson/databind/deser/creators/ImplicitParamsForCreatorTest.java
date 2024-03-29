package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

public class ImplicitParamsForCreatorTest
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
        //@com.fasterxml.jackson.annotation.JsonCreator
        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // [databind#2932]
    static class Bean2932
    {
        int _a, _b;

//        @JsonCreator
        public Bean2932(/*@com.fasterxml.jackson.annotation.JsonProperty("paramName0")*/
                @JsonDeserialize() int a, int b) {
            _a = a;
            _b = b;
        }
    }

    // [databind#3654]: infer "DELEGATING" style from `@JsonValue`
    static class XY3654 {
        public int paramName0; // has to be public to misdirect

        @JsonCreator
        public XY3654(int paramName0) {
            this.paramName0 = paramName0;
        }

        @JsonValue
        public int serializedAs() {
            return paramName0;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .annotationIntrospector(new MyParamIntrospector())
            .build();

    @Test
    public void testNonSingleArgCreator() throws Exception
    {
        XY value = MAPPER.readValue(a2q("{'paramName0':1,'paramName1':2}"), XY.class);
        assertNotNull(value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }

    // [databind#2932]
    @Test
    public void testJsonCreatorWithOtherAnnotations() throws Exception
    {
        Bean2932 bean = MAPPER.readValue(a2q("{'paramName0':1,'paramName1':2}"),
                Bean2932.class);
        assertNotNull(bean);
        assertEquals(1, bean._a);
        assertEquals(2, bean._b);
    }

    // [databind#3654]
    @Test
    public void testDelegatingInferFromJsonValue() throws Exception
    {
        // First verify serialization via `@JsonValue`
        assertEquals("123", MAPPER.writeValueAsString(new XY3654(123)));

        // And then deser, should infer despite existence of "matching" property
        XY3654 result = MAPPER.readValue("345", XY3654.class);
        assertEquals(345, result.paramName0);
    }
}

package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class CreatorNullPrimitives2977Test extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class ABCParamIntrospector extends JacksonAnnotationIntrospector
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

    static class TestClass2977 {
        @JsonProperty("aa")
        final int a;

        // work-around: add JsonProperty on parameter name, or JsonCreator
//        @com.fasterxml.jackson.annotation.JsonCreator
        public TestClass2977(int a) {
            this.a = a;
        }
    }

    // [databind#2977]
    public void testDefaultingWithNull2977() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .annotationIntrospector(new ABCParamIntrospector())
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build();
        TestClass2977 result = mapper.readValue(a2q("{'aa': 8}"), TestClass2977.class);
        assertEquals(8, result.a);
    }
}

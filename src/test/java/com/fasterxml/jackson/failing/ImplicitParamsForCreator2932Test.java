package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ImplicitParamsForCreator2932Test extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                return "paramName"+ap.getIndex();
            }
            return super.findImplicitPropertyName(config, param);
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            // important! Chain to also use standard Jackson annotations
            .annotationIntrospector(new MyParamIntrospector())
            .build();

    // [databind#2932]
    public void testJsonCreatorWithOtherAnnotations() throws Exception
    {
        Bean2932 bean = MAPPER.readValue(a2q("{'paramName0':1,'paramName1':2}"),
                Bean2932.class);
        assertNotNull(bean);
        assertEquals(1, bean._a);
        assertEquals(2, bean._b);
    }
}

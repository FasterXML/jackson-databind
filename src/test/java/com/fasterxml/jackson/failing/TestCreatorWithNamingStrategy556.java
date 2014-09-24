package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class TestCreatorWithNamingStrategy556
    extends BaseMapTest
{
    static class CreatorBean
    {
        protected String myName;
        protected int myAge;

        @JsonCreator
        public CreatorBean(int myAge, String myName)
        {
            this.myName = myName;
            this.myAge = myAge;
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
                case 0: return "myName";
                case 1: return "myAge";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(param);
        }
    }
    
    private final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE)
            ;
    {
        MAPPER.setAnnotationIntrospector(new MyParamIntrospector());
    }

    public void testPascalCaseWithImplicitNames() throws Exception
    {
        CreatorBean bean = MAPPER.readValue("{ \"MyAge\" : 42,  \"MyName\" : \"NotMyRealName\" }", CreatorBean.class);
        assertEquals(42, bean.myAge);
        assertEquals("NotMyRealName", bean.myName);
    }

}
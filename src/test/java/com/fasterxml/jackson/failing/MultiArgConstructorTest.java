package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class MultiArgConstructorTest extends BaseMapTest
{
    static class MultiArgCtorBean
    {
        protected int a, b;

        public MultiArgCtorBean(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    /* Before JDK8, we won't have parameter names available, so let's
     * fake it before that...
     */
    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return "a";
                case 1: return "b";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(param);
        }
    }
    
    /*
    /********************************************************************** 
    /* Test methods
    /********************************************************************** 
     */

    public void testMultiArgVisible() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());
        MultiArgCtorBean bean = mapper.readValue(aposToQuotes("{'b':13,  'a':-99}"),
                MultiArgCtorBean.class);
        assertNotNull(bean);
        assertEquals(13, bean.b);
        assertEquals(-99, bean.a);
    }

    // but let's also ensure that it is possible to prevent use of that constructor
    // with different visibility
    public void testMultiArgNotVisible() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());
        mapper.setVisibility(PropertyAccessor.CREATOR, Visibility.NONE);
        try {
            /*MultiArgCtorBean bean =*/ mapper.readValue(aposToQuotes("{'b':13,  'a':-99}"),
                MultiArgCtorBean.class);
            fail("Should not have passed");
        } catch (JsonMappingException e) {
            verifyException(e, "No suitable constructor");
        }
    }
}

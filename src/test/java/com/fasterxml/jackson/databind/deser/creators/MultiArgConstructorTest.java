package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class MultiArgConstructorTest extends BaseMapTest
{
    static class MultiArgCtorBean
    {
        protected int _a, _b;

        public int c;

        public MultiArgCtorBean(int a, int b) {
            _a = a;
            _b = b;
        }
    }

    static class MultiArgCtorBeanWithAnnotations
    {
        protected int _a, _b;

        public int c;

        public MultiArgCtorBeanWithAnnotations(int a, @JsonProperty("b2") int b) {
            _a = a;
            _b = b;
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
        MultiArgCtorBean bean = mapper.readValue(a2q("{'b':13, 'c':2, 'a':-99}"),
                MultiArgCtorBean.class);
        assertNotNull(bean);
        assertEquals(13, bean._b);
        assertEquals(-99, bean._a);
        assertEquals(2, bean.c);
    }

    // But besides visibility, also allow overrides
    public void testMultiArgWithPartialOverride() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());
        MultiArgCtorBeanWithAnnotations bean = mapper.readValue(a2q("{'b2':7, 'c':222, 'a':-99}"),
                MultiArgCtorBeanWithAnnotations.class);
        assertNotNull(bean);
        assertEquals(7, bean._b);
        assertEquals(-99, bean._a);
        assertEquals(222, bean.c);
    }

    // but let's also ensure that it is possible to prevent use of that constructor
    // with different visibility
    public void testMultiArgNotVisible() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector());
        mapper.setDefaultVisibility(
                JsonAutoDetect.Value.noOverrides()
                    .withCreatorVisibility(Visibility.NONE));
        try {
            /*MultiArgCtorBean bean =*/ mapper.readValue(a2q("{'b':13,  'a':-99}"),
                MultiArgCtorBean.class);
            fail("Should not have passed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no Creators");
        }
    }
}

package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class NamingStrategyViaCreator556Test
    extends BaseMapTest
{
    static class RenamingCtorBean
    {
        protected String myName;
        protected int myAge;

        @JsonCreator
        public RenamingCtorBean(int myAge, String myName)
        {
            this.myName = myName;
            this.myAge = myAge;
        }
    }

    // Try the same with factory, too
    static class RenamedFactoryBean
    {
        protected String myName;
        protected int myAge;

        private RenamedFactoryBean(int a, String n, boolean foo) {
            myAge = a;
            myName = n;
        }

        @JsonCreator
        public static RenamedFactoryBean create(int age, String name) {
            return new RenamedFactoryBean(age, name, true);
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
                case 0: return "myAge";
                case 1: return "myName";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(param);
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .annotationIntrospector(new MyParamIntrospector())
            .build();

    private final static String CTOR_JSON = a2q("{ 'MyAge' : 42,  'MyName' : 'NotMyRealName' }");

    public void testRenameViaCtor() throws Exception
    {
        RenamingCtorBean bean = MAPPER.readValue(CTOR_JSON, RenamingCtorBean.class);
        assertEquals(42, bean.myAge);
        assertEquals("NotMyRealName", bean.myName);
    }

    public void testRenameViaFactory() throws Exception
    {
        RenamedFactoryBean bean = MAPPER.readValue(CTOR_JSON, RenamedFactoryBean.class);
        assertEquals(42, bean.myAge);
        assertEquals("NotMyRealName", bean.myName);
    }
}
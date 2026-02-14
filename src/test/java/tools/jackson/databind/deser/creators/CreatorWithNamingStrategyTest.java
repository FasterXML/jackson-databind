package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

public class CreatorWithNamingStrategyTest
{
    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember param) {
            if (param instanceof AnnotatedParameter ap) {
                return "paramName"+ap.getIndex();
            }
            return super.findImplicitPropertyName(config, param);
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

    // [databind#556]
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

    // [databind#556]
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

    // [databind#556]
    @SuppressWarnings("serial")
    static class NamedParamIntrospector556 extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember param) {
            if (param instanceof AnnotatedParameter ap) {
                switch (ap.getIndex()) {
                case 0: return "myAge";
                case 1: return "myName";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(config, param);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#2051]
    @Test
    public void testSnakeCaseWithOneArg() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .annotationIntrospector(new MyParamIntrospector())
                .build();
        final String MSG = "1st";
        OneProperty actual = mapper.readValue("{\"param_name0\":\""+MSG+"\"}",
                OneProperty.class);
        assertEquals("CTOR:"+MSG, actual.paramName0);
    }

    // [databind#556]
    @Test
    public void testRenameViaCtor() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                .annotationIntrospector(new NamedParamIntrospector556())
                .build();
        final String JSON = a2q("{ 'MyAge' : 42,  'MyName' : 'NotMyRealName' }");
        RenamingCtorBean bean = mapper.readValue(JSON, RenamingCtorBean.class);
        assertEquals(42, bean.myAge);
        assertEquals("NotMyRealName", bean.myName);
    }

    // [databind#556]
    @Test
    public void testRenameViaFactory() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                .annotationIntrospector(new NamedParamIntrospector556())
                .build();
        final String JSON = a2q("{ 'MyAge' : 42,  'MyName' : 'NotMyRealName' }");
        RenamedFactoryBean bean = mapper.readValue(JSON, RenamedFactoryBean.class);
        assertEquals(42, bean.myAge);
        assertEquals("NotMyRealName", bean.myName);
    }
}

package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.introspect.VisibilityChecker;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.DatabindTestUtil.verifyException;

public class MultiArgConstructorTest
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
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return "a";
                case 1: return "b";
                default:
                    return "param"+ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(config, param);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testMultiArgVisible() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new MyParamIntrospector())
                .build();
        MultiArgCtorBean bean = mapper.readValue(a2q("{'b':13, 'c':2, 'a':-99}"),
                MultiArgCtorBean.class);
        assertNotNull(bean);
        assertEquals(13, bean._b);
        assertEquals(-99, bean._a);
        assertEquals(2, bean.c);
    }

    // But besides visibility, also allow overrides
    @Test
    public void testMultiArgWithPartialOverride() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new MyParamIntrospector())
                .build();
        MultiArgCtorBeanWithAnnotations bean = mapper.readValue(a2q("{'b2':7, 'c':222, 'a':-99}"),
                MultiArgCtorBeanWithAnnotations.class);
        assertNotNull(bean);
        assertEquals(7, bean._b);
        assertEquals(-99, bean._a);
        assertEquals(222, bean.c);
    }

    // but let's also ensure that it is possible to prevent use of that constructor
    // with different visibility
    @Test
    public void testMultiArgNotVisible() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new MyParamIntrospector())
                .changeDefaultVisibility(vc -> VisibilityChecker.construct
                        (JsonAutoDetect.Value.noOverrides()
                        .withCreatorVisibility(Visibility.NONE)))
                .build();
        try {
            /*MultiArgCtorBean bean =*/ mapper.readValue(a2q("{'b':13,  'a':-99}"),
                MultiArgCtorBean.class);
            fail("Should not have passed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no Creators");
        }
    }
}

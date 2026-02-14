package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.introspect.VisibilityChecker;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

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

    // Test(s) for "big" creators; ones with at least 32 arguments (sic!).
    // Needed because code paths diverge wrt handling of bitset.
    static class Biggie {
        final int[] stuff;

        @JsonCreator
        public Biggie(
                @JsonProperty("v1") int v1, @JsonProperty("v2") int v2,
                @JsonProperty("v3") int v3, @JsonProperty("v4") int v4,
                @JsonProperty("v5") int v5, @JsonProperty("v6") int v6,
                @JsonProperty("v7") int v7, @JsonProperty("v8") int v8,
                @JsonProperty("v9") int v9, @JsonProperty("v10") int v10,
                @JsonProperty("v11") int v11, @JsonProperty("v12") int v12,
                @JsonProperty("v13") int v13, @JsonProperty("v14") int v14,
                @JsonProperty("v15") int v15, @JsonProperty("v16") int v16,
                @JsonProperty("v17") int v17, @JsonProperty("v18") int v18,
                @JsonProperty("v19") int v19, @JsonProperty("v20") int v20,
                @JsonProperty("v21") int v21, @JsonProperty("v22") int v22,
                @JsonProperty("v23") int v23, @JsonProperty("v24") int v24,
                @JsonProperty("v25") int v25, @JsonProperty("v26") int v26,
                @JsonProperty("v27") int v27, @JsonProperty("v28") int v28,
                @JsonProperty("v29") int v29, @JsonProperty("v30") int v30,
                @JsonProperty("v31") int v31, @JsonProperty("v32") int v32,
                @JsonProperty("v33") int v33, @JsonProperty("v34") int v34,
                @JsonProperty("v35") int v35, @JsonProperty("v36") int v36,
                @JsonProperty("v37") int v37, @JsonProperty("v38") int v38,
                @JsonProperty("v39") int v39, @JsonProperty("v40") int v40
                ) {
            stuff = new int[] {
                    v1, v2, v3, v4, v5, v6, v7, v8, v9, v10,
                    v11, v12, v13, v14, v15, v16, v17, v18, v19, v20,
                    v21, v22, v23, v24, v25, v26, v27, v28, v29, v30,
                    v31, v32, v33, v34, v35, v36, v37, v38, v39, v40,
            };
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
            if (param instanceof AnnotatedParameter ap) {
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

    @Test
    public void testBigPartial() throws Exception
    {
        ObjectReader biggieReader = sharedMapper()
                .readerFor(Biggie.class)
                .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        Biggie value = biggieReader.readValue(a2q(
                "{'v7':7, 'v8':8,'v29':29, 'v35':35}"
                ));
        int[] stuff = value.stuff;
        for (int i = 0; i < stuff.length; ++i) {
            int exp;

            switch (i) {
            case 6: // These are off-by-one...
            case 7:
            case 28:
            case 34:
                exp = i+1;
                break;
            default:
                exp = 0;
            }
            assertEquals(exp, stuff[i], "Entry #"+i);
        }
    }
}

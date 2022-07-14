package tools.jackson.databind.deser.creators;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ImplicitParamsForCreatorTest extends BaseMapTest
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .annotationIntrospector(new MyParamIntrospector())
            .build();

    public void testNonSingleArgCreator() throws Exception
    {
        XY value = MAPPER.readValue(a2q("{'paramName0':1,'paramName1':2}"), XY.class);
        assertNotNull(value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }

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

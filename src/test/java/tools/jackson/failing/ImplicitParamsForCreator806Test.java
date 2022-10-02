package tools.jackson.failing;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class ImplicitParamsForCreator806Test extends BaseMapTest
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
//        @com.fasterxml.jackson.annotation.JsonCreator
        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // for [databind#806]: problem is that renaming occurs too late for implicitly detected
    // Creators
    public void testImplicitNameWithNamingStrategy() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .annotationIntrospector(new MyParamIntrospector())
                .build();
        XY value = mapper.readValue(a2q("{'param_name0':1,'param_name1':2}"), XY.class);
        assertNotNull(value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }
}

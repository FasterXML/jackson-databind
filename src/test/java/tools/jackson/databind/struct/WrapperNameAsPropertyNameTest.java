package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#72] Test for `MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME`
public class WrapperNameAsPropertyNameTest extends DatabindTestUtil
{
    static class WrapperBean {
        public int value;
        public String name;

        public WrapperBean() { }

        public WrapperBean(int value, String name) {
            this.value = value;
            this.name = name;
        }
    }

    // Simple custom AnnotationIntrospector that provides wrapper names
    // for specific properties
    @SuppressWarnings("serial")
    static class WrapperNameIntrospector extends NopAnnotationIntrospector
    {
        @Override
        public PropertyName findWrapperName(MapperConfig<?> config, Annotated ann) {
            if (ann instanceof AnnotatedField field) {
                String fieldName = field.getName();
                if ("value".equals(fieldName)) {
                    return PropertyName.construct("wrappedValue");
                }
                if ("name".equals(fieldName)) {
                    return PropertyName.construct("wrappedName");
                }
            }
            return null;
        }
    }

    private final ObjectMapper MAPPER_WITH_WRAPPER = jsonMapperBuilder()
            .enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)
            .annotationIntrospector(new WrapperNameIntrospector())
            .build();

    private final ObjectMapper MAPPER_WITHOUT_WRAPPER = jsonMapperBuilder()
            .disable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)
            .annotationIntrospector(new WrapperNameIntrospector())
            .build();

    @Test
    public void testSerializationWithWrapperName() throws Exception
    {
        String json = MAPPER_WITH_WRAPPER.writeValueAsString(new WrapperBean(42, "Bob"));
        // Properties should be renamed to wrapper names
        assertEquals(a2q("{'wrappedName':'Bob','wrappedValue':42}"), json);
    }

    @Test
    public void testDeserializationWithWrapperName() throws Exception
    {
        WrapperBean bean = MAPPER_WITH_WRAPPER.readValue(
                a2q("{'wrappedValue':42,'wrappedName':'Bob'}"),
                WrapperBean.class);
        assertEquals(42, bean.value);
        assertEquals("Bob", bean.name);
    }

    @Test
    public void testSerializationWithoutFeature() throws Exception
    {
        // Without feature enabled, original property names should be used
        String json = MAPPER_WITHOUT_WRAPPER.writeValueAsString(new WrapperBean(42, "Bob"));
        assertEquals(a2q("{'name':'Bob','value':42}"), json);
    }

    @Test
    public void testDeserializationWithoutFeature() throws Exception
    {
        // Without feature, original property names are used
        WrapperBean bean = MAPPER_WITHOUT_WRAPPER.readValue(
                a2q("{'value':42,'name':'Bob'}"),
                WrapperBean.class);
        assertEquals(42, bean.value);
        assertEquals("Bob", bean.name);
    }
}

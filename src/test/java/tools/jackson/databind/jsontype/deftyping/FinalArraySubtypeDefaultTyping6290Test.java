package tools.jackson.databind.jsontype.deftyping;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for [databind#6290]: array runtime component type differs
 * from the non-final declared element type.
 */
class FinalArraySubtypeDefaultTyping6290Test extends DatabindTestUtil
{
    public static class Base {
        public int x = 1;
    }

    public static final class FinalSub extends Base { }

    public static class Wrapper {
        public Base[] values;
    }

    public static class AnnotatedBaseArrayWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Base[] values;
    }

    public static class AnnotatedObjectArrayWrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object[] values;
    }

    private ObjectMapper mapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Base[].class)
                        .allowIfBaseType(Base.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
    }

    @Test
    void finalRuntimeArrayElementsRoundTrip() throws Exception {
        ObjectMapper mapper = mapper();
        Wrapper input = new Wrapper();
        input.values = new FinalSub[] { new FinalSub() };

        String json = mapper.writeValueAsString(input);
        Wrapper result = mapper.readValue(json, Wrapper.class);

        assertEquals(FinalSub[].class, result.values.getClass());
        assertEquals(1, result.values.length);
        assertEquals(1, result.values[0].x);
    }

    @Test
    void polymorphicElementsInsideBaseArrayRetainTypeIds() throws Exception {
        ObjectMapper mapper = mapper();
        Wrapper input = new Wrapper();
        input.values = new Base[] { new FinalSub() };

        String json = mapper.writeValueAsString(input);
        assertTrue(json.contains(FinalSub.class.getName()));
        Wrapper result = mapper.readValue(json, Wrapper.class);

        assertEquals(Base[].class, result.values.getClass());
        assertEquals(FinalSub.class, result.values[0].getClass());
    }

    @Test
    void propertyLevelTypeIdsArePreservedWithoutArrayTypeId() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        AnnotatedBaseArrayWrapper input = new AnnotatedBaseArrayWrapper();
        input.values = new FinalSub[] { new FinalSub() };

        String json = mapper.writeValueAsString(input);
        assertTrue(json.contains(FinalSub.class.getName()));

        AnnotatedBaseArrayWrapper result = mapper.readValue(json, AnnotatedBaseArrayWrapper.class);
        assertEquals(1, result.values.length);
        assertEquals(FinalSub.class, result.values[0].getClass());
    }

    @Test
    void propertyLevelObjectArrayIdsPreserveLongValues() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().build();
        AnnotatedObjectArrayWrapper input = new AnnotatedObjectArrayWrapper();
        input.values = new Long[] { 5L };

        String json = mapper.writeValueAsString(input);
        assertTrue(json.contains(Long.class.getName()));

        AnnotatedObjectArrayWrapper result = mapper.readValue(json, AnnotatedObjectArrayWrapper.class);
        assertEquals(1, result.values.length);
        assertEquals(Long.class, result.values[0].getClass());
        assertEquals(Long.valueOf(5L), result.values[0]);
    }
}

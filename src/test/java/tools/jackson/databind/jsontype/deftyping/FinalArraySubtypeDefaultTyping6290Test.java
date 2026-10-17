package tools.jackson.databind.jsontype.deftyping;

import org.junit.jupiter.api.Test;

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
}

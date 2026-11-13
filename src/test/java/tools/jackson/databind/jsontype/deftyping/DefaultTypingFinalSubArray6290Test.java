package tools.jackson.databind.jsontype.deftyping;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6290]: Default typing: `FinalSub[]` value of `Base[]` property
// written with element type ids that fail to deserialize
class DefaultTypingFinalSubArray6290Test extends DatabindTestUtil
{
    static class Base6290 { public int x = 1; }

    static final class FinalSub6290 extends Base6290 { }

    static class NonFinalSub6290 extends Base6290 { }

    static final class Wrapper6290 { public Base6290[] values; }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                    .allowIfBaseType(Base6290[].class)
                    .allowIfBaseType(Base6290.class)
                    .build(), DefaultTyping.NON_FINAL)
            .build();

    @Test
    void finalSubtypeArrayRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new FinalSub6290[] { new FinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        // Elements of final type must not have type ids (since none expected)
        assertEquals("""
                {"values":["[L%s;",[{"x":1}]]}"""
                .formatted(FinalSub6290.class.getName()), json);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(FinalSub6290[].class, result.values.getClass());
        assertEquals(1, result.values.length);
        assertEquals(1, result.values[0].x);
    }

    @Test
    void baseArrayWithFinalSubtypeRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new Base6290[] { new FinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(Base6290[].class, result.values.getClass());
        assertEquals(FinalSub6290.class, result.values[0].getClass());
    }

    @Test
    void nonFinalSubtypeArrayRoundTrip() throws Exception
    {
        Wrapper6290 w = new Wrapper6290();
        w.values = new NonFinalSub6290[] { new NonFinalSub6290() };
        String json = MAPPER.writeValueAsString(w);
        Wrapper6290 result = MAPPER.readValue(json, Wrapper6290.class);
        assertEquals(NonFinalSub6290[].class, result.values.getClass());
        assertEquals(NonFinalSub6290.class, result.values[0].getClass());
    }
}

package tools.jackson.databind.ser;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4762]: `@JsonValue` should honor `@JsonInclude` declared on the annotated accessor
class JsonValueIgnoresJsonInclude4762Test extends DatabindTestUtil
{
    static class JsonValueWithInclude {
        @JsonValue
        @JsonInclude(value = JsonInclude.Include.NON_NULL,
                content = JsonInclude.Include.NON_NULL)
        public final Map<String, Object> value;

        JsonValueWithInclude(Map<String, Object> value) {
            this.value = value;
        }
    }

    // Enclosing property with no inclusion override of its own: the accessor-level
    // `@JsonInclude` must still be honored when contextualized against a (non-null)
    // enclosing `BeanProperty`.
    static class Wrapper {
        public JsonValueWithInclude wrapped;

        Wrapper(JsonValueWithInclude wrapped) {
            this.wrapped = wrapped;
        }
    }

    // Enclosing property that overrides content inclusion to ALWAYS: per the established
    // precedence ([databind#2822]) the enclosing property configuration wins, re-including
    // the null map values that the accessor-level `@JsonInclude` would otherwise drop.
    static class WrapperContentAlways {
        @JsonInclude(content = JsonInclude.Include.ALWAYS)
        public JsonValueWithInclude wrapped;

        WrapperContentAlways(JsonValueWithInclude wrapped) {
            this.wrapped = wrapped;
        }
    }

    // [databind#4762]: accessor annotations must also be honored when the `@JsonValue`
    // value type is `final` (here `BigDecimal`), which takes the static-typing branch of
    // `createContextual` rather than the dynamic one.
    static class FinalValueWithFormat {
        @JsonValue
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public final BigDecimal value;

        FinalValueWithFormat(BigDecimal value) {
            this.value = value;
        }
    }

    // Enclosing property overriding the format back to NUMBER: [databind#2822] precedence
    // must hold in the static-typing branch too, so the enclosing override wins.
    static class WrapperFormatNumber {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public FinalValueWithFormat wrapped;

        WrapperFormatNumber(FinalValueWithFormat wrapped) {
            this.wrapped = wrapped;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private Map<String, Object> mapWithNull() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("present", "x");
        map.put("missing", null);
        return map;
    }

    @Test
    void jsonValueShouldHonorJsonIncludeOnField() throws Exception {
        String json = MAPPER.writeValueAsString(new JsonValueWithInclude(mapWithNull()));

        // Expected: `@JsonInclude(content = NON_NULL)` on the `@JsonValue` field
        // should drop null map values.
        assertEquals("{\"present\":\"x\"}", json);
    }

    // [databind#4762]: accessor-level `@JsonInclude` must be honored even when the
    // `@JsonValue` type is nested behind an enclosing property (exercises the
    // `_enclosing != null` contextualization path).
    @Test
    void jsonValueShouldHonorJsonIncludeWhenNested() throws Exception {
        String json = MAPPER.writeValueAsString(new Wrapper(new JsonValueWithInclude(mapWithNull())));

        assertEquals("{\"wrapped\":{\"present\":\"x\"}}", json);
    }

    // [databind#2822]: configuration on the enclosing property still takes precedence over
    // accessor-level configuration, so a `content = ALWAYS` override re-includes null values.
    @Test
    void enclosingPropertyInclusionShouldOverrideAccessor() throws Exception {
        String json = MAPPER.writeValueAsString(new WrapperContentAlways(new JsonValueWithInclude(mapWithNull())));

        assertEquals("{\"wrapped\":{\"present\":\"x\",\"missing\":null}}", json);
    }

    // [databind#4762]: accessor-level `@JsonFormat` must be honored for a `final` value
    // type (static-typing branch), so the `BigDecimal` is written as a String.
    @Test
    void jsonValueShouldHonorJsonFormatOnFinalAccessor() throws Exception {
        String json = MAPPER.writeValueAsString(new FinalValueWithFormat(BigDecimal.ONE));

        assertEquals("\"1\"", json);
    }

    // [databind#2822]: enclosing-property configuration still wins in the static-typing
    // branch, so the enclosing `@JsonFormat(NUMBER)` overrides the accessor's STRING.
    @Test
    void enclosingPropertyFormatShouldOverrideAccessor() throws Exception {
        String json = MAPPER.writeValueAsString(new WrapperFormatNumber(new FinalValueWithFormat(BigDecimal.ONE)));

        assertEquals("{\"wrapped\":1}", json);
    }
}

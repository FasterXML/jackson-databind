package tools.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5380]: `@JsonValue` should honor inclusion configured (via config
// override) for the type it serializes as
class JsonValueInclusion5380Test extends DatabindTestUtil
{
    record JsonValuePojo(String stringValue) {
        @JsonValue
        public String toString() { return stringValue; }
    }

    // non-final variant, to exercise dynamic serializer lookup
    static class NonFinalValue {
        private final String value;

        NonFinalValue(String value) { this.value = value; }

        @JsonValue
        public String value() { return value; }
    }

    record WrapperPojo(JsonValuePojo jsonValuePojo) { }

    record NonFinalWrapper(NonFinalValue value) { }

    record StringPojo(String stringValue) { }

    record AnnotatedWrapper(@JsonInclude(JsonInclude.Include.NON_EMPTY) JsonValuePojo jsonValuePojo) { }

    // `@JsonInclude` on the `@JsonValue` accessor itself
    record AccessorAnnotatedValue(String value) {
        @JsonValue
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public String toString() { return value; }
    }

    record AccessorAnnotatedWrapper(AccessorAnnotatedValue value) { }

    // Nested `@JsonValue`: outer serializes as inner, which serializes as String
    record OuterValue(JsonValuePojo inner) {
        @JsonValue
        public JsonValuePojo inner() { return inner; }
    }

    record NestedWrapper(@JsonInclude(JsonInclude.Include.NON_EMPTY) OuterValue value) { }

    record MapWrapper(@JsonInclude(content = JsonInclude.Include.NON_EMPTY)
            Map<String, JsonValuePojo> values) { }

    private final ObjectMapper MAPPER = mapperBuilder()
            .withConfigOverride(String.class, o -> o.setIncludeAsProperty(
                    JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)))
            .build();

    private static JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY));
    }

    @Test
    void plainStringProperty() throws Exception {
        assertEquals("""
                {"stringValue":""}""", MAPPER.writeValueAsString(new StringPojo("")));
    }

    @Test
    void jsonValueUsesValueTypeOverride() throws Exception {
        assertEquals("""
                {"jsonValuePojo":""}""",
                MAPPER.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));
        assertEquals("""
                {"jsonValuePojo":"x"}""",
                MAPPER.writeValueAsString(new WrapperPojo(new JsonValuePojo("x"))));
        // nulls still suppressed, as per NON_NULL
        assertEquals("{}", MAPPER.writeValueAsString(new WrapperPojo(null)));
        assertEquals("{}", MAPPER.writeValueAsString(new WrapperPojo(new JsonValuePojo(null))));
    }

    @Test
    void jsonValueNonFinalUsesValueTypeOverride() throws Exception {
        assertEquals("""
                {"value":""}""",
                MAPPER.writeValueAsString(new NonFinalWrapper(new NonFinalValue(""))));
    }

    // Without override for value type, global NON_EMPTY applies
    @Test
    void jsonValueWithoutOverride() throws Exception {
        ObjectMapper mapper = mapperBuilder().build();
        assertEquals("{}", mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));
    }

    // Explicit annotation on property has precedence over value type override
    @Test
    void propertyAnnotationHasPrecedence() throws Exception {
        assertEquals("{}",
                MAPPER.writeValueAsString(new AnnotatedWrapper(new JsonValuePojo(""))));
    }

    // As does annotation on the `@JsonValue` accessor
    @Test
    void accessorAnnotationHasPrecedence() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(
                new AccessorAnnotatedWrapper(new AccessorAnnotatedValue(""))));
    }

    // Explicit property annotation must also win for nested `@JsonValue`
    @Test
    void nestedJsonValueHonorsPropertyAnnotation() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(
                new NestedWrapper(new OuterValue(new JsonValuePojo("")))));
        assertEquals("""
                {"value":"x"}""", MAPPER.writeValueAsString(
                new NestedWrapper(new OuterValue(new JsonValuePojo("x")))));
    }

    // As does override for declared property type
    @Test
    void declaredTypeOverrideHasPrecedence() throws Exception {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(String.class, o -> o.setIncludeAsProperty(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null)))
                .withConfigOverride(JsonValuePojo.class, o -> o.setIncludeAsProperty(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null)))
                .build();
        assertEquals("{}", mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));
    }

    // Content inclusion of container properties not affected
    @Test
    void mapContentInclusionNotAffected() throws Exception {
        Map<String, JsonValuePojo> values = new LinkedHashMap<>();
        values.put("a", new JsonValuePojo(""));
        values.put("b", new JsonValuePojo("x"));
        assertEquals("""
                {"values":{"b":"x"}}""", MAPPER.writeValueAsString(new MapWrapper(values)));
    }
}

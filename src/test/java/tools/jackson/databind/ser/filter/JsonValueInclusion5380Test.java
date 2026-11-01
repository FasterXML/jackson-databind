package tools.jackson.databind.ser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.ValueSerializerModifier;
import tools.jackson.databind.ser.jackson.JsonValueSerializer;
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

    record AccessorAlwaysValue(String value) {
        @JsonValue
        @JsonInclude(JsonInclude.Include.ALWAYS)
        public String toString() { return value; }
    }

    record AccessorAlwaysWrapper(AccessorAlwaysValue value) { }

    record AnnotatedAccessorAlwaysWrapper(
            @JsonInclude(JsonInclude.Include.NON_EMPTY) AccessorAlwaysValue value) { }

    // Nested `@JsonValue`: outer serializes as inner, which serializes as String
    record OuterValue(JsonValuePojo inner) {
        @JsonValue
        public JsonValuePojo inner() { return inner; }
    }

    record NestedWrapper(@JsonInclude(JsonInclude.Include.NON_EMPTY) OuterValue value) { }

    // Non-Record wrapper, needed for global NON_DEFAULT to fall back to "empty" check
    static class PojoWrapper {
        public JsonValuePojo value;

        PojoWrapper(JsonValuePojo value) { this.value = value; }
    }

    // Custom sub-class: must be retained through contextualization
    static class CustomJsonValueSerializer extends JsonValueSerializer {
        CustomJsonValueSerializer(JsonValueSerializer src) {
            super(src, null, null, null, true);
        }

        CustomJsonValueSerializer(CustomJsonValueSerializer src, BeanProperty property,
                TypeSerializer vts, ValueSerializer<?> ser, boolean forceTypeInfo,
                boolean emptyCheckDisabled) {
            super(src, property, vts, ser, forceTypeInfo, emptyCheckDisabled);
        }

        @Override
        public JsonValueSerializer withResolved(BeanProperty property,
                TypeSerializer vts, ValueSerializer<?> ser, boolean forceTypeInfo) {
            return new CustomJsonValueSerializer(this, property, vts, ser, forceTypeInfo,
                    _emptyCheckDisabled);
        }

        @Override
        public JsonValueSerializer withEmptyCheckDisabled(boolean state) {
            return new CustomJsonValueSerializer(this, _property, _valueTypeSerializer,
                    _valueSerializer, _forceTypeInformation, state);
        }

        @Override
        public void serialize(Object bean, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("custom:" + bean);
        }
    }

    record MapWrapper(@JsonInclude(content = JsonInclude.Include.NON_EMPTY)
            Map<String, JsonValuePojo> values) { }

    record ObjectMapWrapper(@JsonInclude(content = JsonInclude.Include.NON_EMPTY)
            Object values) { }

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

    // Accessor annotation that does not exclude empty values is used as well;
    // with or without value type override
    @Test
    void accessorAnnotationAllowingEmpty() throws Exception {
        assertEquals("""
                {"value":""}""", MAPPER.writeValueAsString(
                new AccessorAlwaysWrapper(new AccessorAlwaysValue(""))));
        assertEquals("""
                {"value":""}""", mapperBuilder().build().writeValueAsString(
                new AccessorAlwaysWrapper(new AccessorAlwaysValue(""))));
    }

    // ... but annotation on enclosing property has precedence over accessor annotation
    @Test
    void propertyAnnotationBeatsAccessorAnnotation() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(
                new AnnotatedAccessorAlwaysWrapper(new AccessorAlwaysValue(""))));
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

    // Override "include" (not just "include as property") for declared type is
    // also used for property inclusion, so it too has precedence
    @Test
    void declaredTypeIncludeOverrideHasPrecedence() throws Exception {
        ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(String.class, o -> o.setIncludeAsProperty(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null)))
                .withConfigOverride(JsonValuePojo.class, o -> o.setInclude(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null)))
                .build();
        assertEquals("{}", mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));
    }

    // Global NON_DEFAULT falls back to "empty" check for POJO-valued property
    @Test
    void nonDefaultInclusion() throws Exception {
        JsonMapper.Builder b = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> JsonInclude.Value.construct(
                        JsonInclude.Include.NON_DEFAULT, JsonInclude.Include.NON_DEFAULT));
        assertEquals("{}", b.build().writeValueAsString(new PojoWrapper(new JsonValuePojo(""))));

        ObjectMapper mapper = b.withConfigOverride(String.class, o -> o.setIncludeAsProperty(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null)))
                .build();
        assertEquals("""
                {"value":""}""", mapper.writeValueAsString(new PojoWrapper(new JsonValuePojo(""))));
        assertEquals("{}", mapper.writeValueAsString(new PojoWrapper(null)));
    }

    // Custom sub-class retained, with and without empty check disabled
    @Test
    void customSubclassRetained() throws Exception {
        SimpleModule mod = new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
            private static final long serialVersionUID = 1L;

            @Override
            public ValueSerializer<?> modifySerializer(SerializationConfig config,
                    BeanDescription.Supplier beanDesc, ValueSerializer<?> serializer) {
                if (serializer instanceof JsonValueSerializer jvs) {
                    return new CustomJsonValueSerializer(jvs);
                }
                return serializer;
            }
        });
        // No override for value type: empty value suppressed
        ObjectMapper mapper = mapperBuilder().addModule(mod).build();
        assertEquals("""
                {"jsonValuePojo":"custom:x"}""",
                mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo("x"))));
        assertEquals("{}", mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));

        // With override: empty value retained, custom serializer still used
        mapper = mapperBuilder().addModule(mod)
                .withConfigOverride(String.class, o -> o.setIncludeAsProperty(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, null)))
                .build();
        assertEquals("""
                {"jsonValuePojo":"custom:"}""",
                mapper.writeValueAsString(new WrapperPojo(new JsonValuePojo(""))));
    }

    // Content inclusion of container properties not affected
    @Test
    void mapContentInclusionNotAffected() throws Exception {
        Map<String, JsonValuePojo> values = new LinkedHashMap<>();
        values.put("a", new JsonValuePojo(""));
        values.put("b", new JsonValuePojo("x"));
        assertEquals("""
                {"values":{"b":"x"}}""", MAPPER.writeValueAsString(new MapWrapper(values)));
        // including case of `Object`-typed property
        assertEquals("""
                {"values":{"b":"x"}}""", MAPPER.writeValueAsString(new ObjectMapWrapper(values)));
    }
}

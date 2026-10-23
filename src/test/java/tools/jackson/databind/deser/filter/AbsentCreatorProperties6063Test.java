package tools.jackson.databind.deser.filter;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.DeserializationFeature.*;

class AbsentCreatorProperties6063Test
{
    private static final String NULL_VALUE_JSON = """
            {"value":null}
            """;

    private static final String PRESENT_VALUE_JSON = """
            {"value":"present"}
            """;

    record Value(String value) { }

    record PrimitiveValue(int value) { }

    record ListValue(List<String> value) { }

    record RequiredValue(@JsonProperty(required = true) String value) { }

    record AnnotatedValue(@JsonSetter(nulls = Nulls.FAIL) String value) { }

    record InjectedValue(@JacksonInject("value") String value) { }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    record ArrayValue(String value) { }

    static class ConstructorValue {
        final String value;

        @JsonCreator
        ConstructorValue(@JsonProperty("value") String value) {
            this.value = value;
        }
    }

    static class FactoryValue {
        final String value;

        private FactoryValue(String value) {
            this.value = value;
        }

        @JsonCreator
        static FactoryValue create(@JsonProperty("value") String value) {
            return new FactoryValue(value);
        }
    }

    record CustomValue(@JsonDeserialize(using = AbsentStringDeserializer.class) String value) { }

    static class AbsentStringDeserializer extends StdDeserializer<String> {
        AbsentStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString();
        }

        @Override
        public Object getAbsentValue(DeserializationContext ctxt) {
            return "absent";
        }
    }

    private final ObjectMapper FAIL_MAPPER = mapper(Nulls.FAIL);

    private ObjectMapper mapper(Nulls nulls) {
        return JsonMapper.builder()
                .changeDefaultNullHandling(h -> h.withValueNulls(nulls))
                .build();
    }

    @Test
    void defaultStillRejectsMissingAndExplicitNull() {
        assertTrue(FAIL_MAPPER.isEnabled(ABSENT_BEHAVES_LIKE_NULL));
        ObjectReader reader = FAIL_MAPPER.readerFor(Value.class);
        assertThrows(InvalidNullException.class, () -> reader.readValue("{}"));
        assertThrows(InvalidNullException.class, () -> reader.readValue(NULL_VALUE_JSON));
    }

    @Test
    void missingRecordPropertyCanUseDefaultWhileExplicitNullStillFails() {
        ObjectReader reader = FAIL_MAPPER.readerFor(Value.class).without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.<Value>readValue("{}").value());
        assertEquals("present", reader.<Value>readValue(PRESENT_VALUE_JSON).value());
        assertThrows(InvalidNullException.class, () -> reader.readValue(NULL_VALUE_JSON));
    }

    @Test
    void missingConstructorAndFactoryArgumentsUseDefault() {
        ObjectReader reader = FAIL_MAPPER.reader().without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.forType(ConstructorValue.class).<ConstructorValue>readValue("{}").value);
        assertNull(reader.forType(FactoryValue.class).<FactoryValue>readValue("{}").value);
    }

    @Test
    void mapperCanDisableFeature() {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(ABSENT_BEHAVES_LIKE_NULL)
                .changeDefaultNullHandling(h -> h.withValueNulls(Nulls.FAIL))
                .build();
        assertNull(mapper.readValue("{}", Value.class).value());
    }

    @Test
    void readerOverridesDoNotChangeCachedDeserializerBehaviorForOtherReaders() {
        ObjectReader strict = FAIL_MAPPER.readerFor(Value.class);
        ObjectReader relaxed = strict.without(ABSENT_BEHAVES_LIKE_NULL);
        assertThrows(InvalidNullException.class, () -> strict.readValue("{}"));
        assertNull(relaxed.<Value>readValue("{}").value());
        assertThrows(InvalidNullException.class, () -> strict.readValue("{}"));
        assertThrows(InvalidNullException.class,
                () -> relaxed.with(ABSENT_BEHAVES_LIKE_NULL).readValue("{}"));
        assertNull(relaxed.<Value>readValue("{}").value());
    }

    @Test
    void primitiveDefaultsArePreserved() {
        ObjectReader reader = FAIL_MAPPER.readerFor(PrimitiveValue.class)
                .without(ABSENT_BEHAVES_LIKE_NULL);
        assertEquals(0, reader.<PrimitiveValue>readValue("{}").value());
        assertThrows(InvalidNullException.class, () -> reader.readValue(NULL_VALUE_JSON));
    }

    @Test
    void propertyAnnotationStillRejectsExplicitNull() {
        ObjectReader reader = JsonMapper.builder().build().readerFor(AnnotatedValue.class)
                .without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.<AnnotatedValue>readValue("{}").value());
        assertThrows(InvalidNullException.class, () -> reader.readValue(NULL_VALUE_JSON));
    }

    @Test
    void requiredPropertyStillFails() {
        ObjectReader reader = FAIL_MAPPER.readerFor(RequiredValue.class)
                .without(ABSENT_BEHAVES_LIKE_NULL);
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue("{}"));
        assertTrue(e.getMessage().contains("Missing required creator property 'value'"));
    }

    @ParameterizedTest
    @EnumSource(value = DeserializationFeature.class, names = {
            "FAIL_ON_MISSING_CREATOR_PROPERTIES", "FAIL_ON_NULL_CREATOR_PROPERTIES" })
    void strictCreatorFeaturesStillFail(DeserializationFeature feature) {
        ObjectReader reader = FAIL_MAPPER.readerFor(Value.class)
                .without(ABSENT_BEHAVES_LIKE_NULL).with(feature);
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue("{}"));
        assertTrue(e.getMessage().contains(feature.name()));
    }

    @Test
    void asEmptyOnlyAppliesToExplicitNullWhenFeatureIsDisabled() {
        ObjectReader defaults = mapper(Nulls.AS_EMPTY).readerFor(ListValue.class);
        assertEquals(List.of(), defaults.<ListValue>readValue("{}").value());
        ObjectReader reader = defaults.without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.<ListValue>readValue("{}").value());
        assertEquals(List.of(), reader.<ListValue>readValue(NULL_VALUE_JSON).value());
    }

    @ParameterizedTest
    @EnumSource(value = Nulls.class, names = { "SET", "SKIP" })
    void setAndSkipContinueToUseAbsentDefault(Nulls nulls) {
        ObjectReader reader = mapper(nulls).readerFor(Value.class).without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.<Value>readValue("{}").value());
        assertNull(reader.<Value>readValue(NULL_VALUE_JSON).value());
    }

    @Test
    void arrayShapedCreatorUsesAbsentDefault() {
        ObjectReader reader = FAIL_MAPPER.readerFor(ArrayValue.class).without(ABSENT_BEHAVES_LIKE_NULL);
        assertNull(reader.<ArrayValue>readValue("[]").value());
        assertThrows(InvalidNullException.class, () -> reader.readValue("[null]"));
    }

    @Test
    void customDeserializerStillSuppliesAbsentValue() {
        ObjectReader reader = FAIL_MAPPER.readerFor(CustomValue.class).without(ABSENT_BEHAVES_LIKE_NULL);
        assertEquals("absent", reader.<CustomValue>readValue("{}").value());
        assertThrows(InvalidNullException.class, () -> reader.readValue(NULL_VALUE_JSON));
    }

    @Test
    void injectableValueTakesPrecedence() {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(ABSENT_BEHAVES_LIKE_NULL)
                .changeDefaultNullHandling(h -> h.withValueNulls(Nulls.FAIL))
                .injectableValues(new InjectableValues.Std().addValue("value", "injected"))
                .build();
        assertEquals("injected", mapper.readValue("{}", InjectedValue.class).value());
    }
}

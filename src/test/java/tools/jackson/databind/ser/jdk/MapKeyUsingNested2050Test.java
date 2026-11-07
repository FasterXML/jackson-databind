package tools.jackson.databind.ser.jdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapKeyUsingNested2050Test
{
    record ObjectKey(int id, String name) {
        @Override
        public String toString() {
            return "default-" + id;
        }
    }

    static class ObjectKeySerializer extends ValueSerializer<ObjectKey> {
        @Override
        public void serialize(ObjectKey key, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName(key.id() + ": " + key.name());
        }
    }

    static class Envelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class)
        public Map<ObjectKey, Object> attributes;

        Envelope(Object value) {
            attributes = Map.of(new ObjectKey(1, "First"), value);
        }
    }

    static class TypedEnvelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class)
        public Map<ObjectKey, Map<String, String>> attributes =
                Map.of(new ObjectKey(1, "First"), Map.of("key", "value"));
    }

    static class OptionalEnvelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class)
        public Optional<Map<ObjectKey, Object>> attributes = Optional.of(new Envelope(1).attributes);
    }

    static class IdentityMapConverter extends StdConverter<Map<ObjectKey, Object>, Map<ObjectKey, Object>> {
        @Override
        public Map<ObjectKey, Object> convert(Map<ObjectKey, Object> value) {
            return value;
        }
    }

    static class ConvertedEnvelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class, converter = IdentityMapConverter.class)
        public Map<ObjectKey, Object> attributes = new Envelope(1).attributes;
    }

    record Payload(String value) { }

    static class PayloadConverter extends StdConverter<Payload, Map<String, String>> {
        @Override
        public Map<String, String> convert(Payload value) {
            return Map.of("key", value.value());
        }
    }

    static class ConvertedContentEnvelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class, contentConverter = PayloadConverter.class)
        public Map<ObjectKey, Payload> attributes =
                Map.of(new ObjectKey(1, "First"), new Payload("value"));
    }

    static class IntegerValueSerializer extends ValueSerializer<Integer> {
        @Override
        public void serialize(Integer value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("value-" + value);
        }
    }

    static class CustomContentEnvelope {
        @JsonSerialize(keyUsing = ObjectKeySerializer.class, contentUsing = IntegerValueSerializer.class)
        public Map<ObjectKey, Integer> attributes = Map.of(new ObjectKey(1, "First"), 1);
    }

    static class PlainEnvelope {
        public Map<ObjectKey, Object> attributes;

        PlainEnvelope(Object value) {
            attributes = new Envelope(value).attributes;
        }
    }

    static class StringKeySerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("custom-" + value);
        }
    }

    static class ConvertibleValue {
        final int number;

        ConvertibleValue(int number) {
            this.number = number;
        }
    }

    static class ToMapConverter extends StdConverter<ConvertibleValue, Map<String, Integer>> {
        @Override
        public Map<String, Integer> convert(ConvertibleValue value) {
            return Map.of("key", value.number);
        }
    }

    static class OptionalConvertedContent {
        @JsonSerialize(keyUsing = StringKeySerializer.class, contentConverter = ToMapConverter.class)
        public Optional<ConvertibleValue> attributes = Optional.of(new ConvertibleValue(3));
    }

    static class ListConvertedContent {
        @JsonSerialize(keyUsing = StringKeySerializer.class, contentConverter = ToMapConverter.class)
        public List<ConvertibleValue> attributes = List.of(new ConvertibleValue(3));
    }

    static class ReferenceConvertedContent {
        @JsonSerialize(keyUsing = StringKeySerializer.class, contentConverter = ToMapConverter.class)
        public AtomicReference<ConvertibleValue> attributes = new AtomicReference<>(new ConvertibleValue(3));
    }

    @JsonSerialize(keyUsing = StringKeySerializer.class)
    static class AnnotatedMap extends LinkedHashMap<String, Integer> {
        AnnotatedMap() {
            put("key", 1);
        }
    }

    static class SortedEnvelope {
        @JsonSerialize(keyUsing = StringKeySerializer.class)
        @JsonFormat(with = JsonFormat.Feature.WRITE_SORTED_MAP_ENTRIES)
        public Map<String, Object> attributes = new LinkedHashMap<>();

        SortedEnvelope() {
            Map<String, Integer> inner = new LinkedHashMap<>();
            inner.put("b", 2);
            inner.put("a", 1);
            attributes.put("key", inner);
        }
    }

    static class NonNullEnvelope {
        @JsonSerialize(keyUsing = StringKeySerializer.class)
        @JsonInclude(content = JsonInclude.Include.NON_NULL)
        public Map<String, Object> attributes = new LinkedHashMap<>();

        NonNullEnvelope() {
            attributes.put("missing", null);
            attributes.put("present", 3);
        }
    }

    static class SameKeyTypedEnvelope {
        @JsonSerialize(keyUsing = StringKeySerializer.class)
        public Map<String, Map<String, Integer>> attributes = Map.of("outer", Map.of("inner", 3));
    }

    static class ObjectTypedEnvelope {
        @JsonSerialize(keyUsing = StringKeySerializer.class)
        public Object attributes = Map.of("outer", Map.of("inner", 3));
    }

    static class AnyGetterEnvelope {
        @JsonAnyGetter
        @JsonSerialize(keyUsing = StringKeySerializer.class)
        public Map<String, Object> attributes() {
            return Map.of("outer", Map.of("inner", 3));
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.TYPE})
    @interface Prefix {
        String value();
    }

    static class ContextualValueSerializer extends ValueSerializer<ConvertibleValue> {
        private final String prefix;

        public ContextualValueSerializer() {
            this("unset");
        }

        ContextualValueSerializer(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            return new ContextualValueSerializer(property.getName() + ":"
                    + property.getAnnotation(Prefix.class).value() + ":"
                    + property.getContextAnnotation(Prefix.class).value());
        }

        @Override
        public void serialize(ConvertibleValue value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(prefix + ":" + value.number);
        }
    }

    @Prefix("class")
    static class ContextualEnvelope {
        @Prefix("field")
        @JsonSerialize(keyUsing = StringKeySerializer.class, contentUsing = ContextualValueSerializer.class)
        public Map<String, ConvertibleValue> attributes = Map.of("key", new ConvertibleValue(3));
    }

    static class ModuleKeySerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String key, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("module-" + key);
        }
    }

    @Test
    void scalarValue() {
        assertEquals("""
                {"attributes":{"1: First":1}}""",
                JsonMapper.builder().build().writeValueAsString(new Envelope(1)));
    }

    @Test
    void nestedMapUsesItsOwnKeySerializer() {
        assertEquals("""
                {"attributes":{"1: First":{"key":"value"}}}""",
                JsonMapper.builder().build().writeValueAsString(new Envelope(Map.of("key", "value"))));
    }

    @Test
    void sameKeyTypeDoesNotReusePropertySerializer() {
        assertEquals("""
                {"attributes":{"1: First":{"default-2":2}}}""",
                JsonMapper.builder().build().writeValueAsString(
                        new Envelope(Map.of(new ObjectKey(2, "Second"), 2))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void typedNestedMap(boolean staticTyping) {
        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_STATIC_TYPING, staticTyping)
                .build();
        assertEquals("""
                {"attributes":{"1: First":{"key":"value"}}}""",
                mapper.writeValueAsString(new TypedEnvelope()));
    }

    @Test
    void threeMapLevels() {
        assertEquals("""
                {"attributes":{"1: First":{"second":{"third":3}}}}""",
                JsonMapper.builder().build().writeValueAsString(
                        new Envelope(Map.of("second", Map.of("third", 3)))));
    }

    @Test
    void mapInsideList() {
        assertEquals("""
                {"attributes":{"1: First":[{"key":"value"}]}}""",
                JsonMapper.builder().build().writeValueAsString(
                        new Envelope(List.of(Map.of("key", "value")))));
    }

    @Test
    void mapInsideOptional() {
        assertEquals("""
                {"attributes":{"1: First":{"key":"value"}}}""",
                JsonMapper.builder().build().writeValueAsString(
                        new Envelope(Optional.of(Map.of("key", "value")))));
    }

    @Test
    void propertyAnnotationAppliesThroughOptional() {
        assertEquals("""
                {"attributes":{"1: First":1}}""",
                JsonMapper.builder().build().writeValueAsString(new OptionalEnvelope()));
    }

    @Test
    void propertyAnnotationAppliesThroughConverter() {
        assertEquals("""
                {"attributes":{"1: First":1}}""",
                JsonMapper.builder().build().writeValueAsString(new ConvertedEnvelope()));
    }

    @Test
    void mapProducedByContentConverter() {
        assertEquals("""
                {"attributes":{"1: First":{"key":"value"}}}""",
                JsonMapper.builder().build().writeValueAsString(new ConvertedContentEnvelope()));
    }

    @Test
    void optionalContentConverterIsNotAppliedToMapValues() {
        assertEquals("""
                {"attributes":{"custom-key":3}}""",
                JsonMapper.builder().build().writeValueAsString(new OptionalConvertedContent()));
    }

    @Test
    void listContentConverterIsNotAppliedToMapValues() {
        assertEquals("""
                {"attributes":[{"custom-key":3}]}""",
                JsonMapper.builder().build().writeValueAsString(new ListConvertedContent()));
    }

    @Test
    void referenceContentConverterIsNotAppliedToMapValues() {
        assertEquals("""
                {"attributes":{"custom-key":3}}""",
                JsonMapper.builder().build().writeValueAsString(new ReferenceConvertedContent()));
    }

    @Test
    void explicitContentSerializerIsRetained() {
        assertEquals("""
                {"attributes":{"1: First":"value-1"}}""",
                JsonMapper.builder().build().writeValueAsString(new CustomContentEnvelope()));
    }

    @Test
    void nestedBeanUsesItsOwnPropertyAnnotation() {
        assertEquals("""
                {"attributes":{"1: First":{"attributes":{"1: First":1}}}}""",
                JsonMapper.builder().build().writeValueAsString(new Envelope(new Envelope(1))));
    }

    @Test
    void nestedMapKeepsClassAnnotation() {
        assertEquals("""
                {"attributes":{"1: First":{"custom-key":1}}}""",
                JsonMapper.builder().build().writeValueAsString(new Envelope(new AnnotatedMap())));
    }

    @Test
    void retainsOtherPropertyMetadata() {
        assertEquals("""
                {"attributes":{"custom-key":{"a":1,"b":2}}}""",
                JsonMapper.builder().build().writeValueAsString(new SortedEnvelope()));
    }

    @Test
    void retainsContentInclusion() {
        assertEquals("""
                {"attributes":{"custom-present":3}}""",
                JsonMapper.builder().build().writeValueAsString(new NonNullEnvelope()));
    }

    @Test
    void sameDeclaredKeyTypeDoesNotReusePropertySerializer() {
        assertEquals("""
                {"attributes":{"custom-outer":{"inner":3}}}""",
                JsonMapper.builder().build().writeValueAsString(new SameKeyTypedEnvelope()));
    }

    @Test
    void propertyAnnotationAppliesToObjectTypedMap() {
        assertEquals("""
                {"attributes":{"custom-outer":{"inner":3}}}""",
                JsonMapper.builder().build().writeValueAsString(new ObjectTypedEnvelope()));
    }

    @Test
    void anyGetterMapDoesNotPropagateKeySerializer() {
        assertEquals("""
                {"custom-outer":{"inner":3}}""",
                JsonMapper.builder().build().writeValueAsString(new AnyGetterEnvelope()));
    }

    @Test
    void contentSerializerRetainsPropertyAndClassAnnotations() {
        assertEquals("""
                {"attributes":{"custom-key":"attributes:field:class:3"}}""",
                JsonMapper.builder().build().writeValueAsString(new ContextualEnvelope()));
    }

    @Test
    void nestedMapUsesModuleKeySerializer() {
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(String.class, new ModuleKeySerializer());
        assertEquals("""
                {"attributes":{"custom-outer":{"module-inner":3}}}""",
                JsonMapper.builder().addModule(module).build().writeValueAsString(new SameKeyTypedEnvelope()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void independentPropertyAndRootCaches(boolean warmPlainFirst) {
        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, String> nested = Map.of("key", "value");
        PlainEnvelope plain = new PlainEnvelope(nested);
        Envelope annotated = new Envelope(nested);
        String expectedPlain = """
                {"attributes":{"default-1":{"key":"value"}}}""";
        String expectedAnnotated = """
                {"attributes":{"1: First":{"key":"value"}}}""";
        if (warmPlainFirst) {
            assertEquals(expectedPlain, mapper.writeValueAsString(plain));
        }
        for (int i = 0; i < 3; ++i) {
            assertEquals(expectedAnnotated, mapper.writeValueAsString(annotated));
            assertEquals(expectedPlain, mapper.writeValueAsString(plain));
            assertEquals("""
                    {"key":"value"}""", mapper.writeValueAsString(nested));
        }
    }
}

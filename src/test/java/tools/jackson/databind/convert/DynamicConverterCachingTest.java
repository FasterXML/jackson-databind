package tools.jackson.databind.convert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicConverterCachingTest {
    @JsonSerialize(converter = UntypedConverter.class)
    static class Input {
        final Object value;
        Input(Object value) { this.value = value; }
    }

    static class UntypedConverter extends StdConverter<Input, Object> {
        @Override
        public Object convert(Input value) { return value.value; }
    }

    static class Payload {
        final String text;
        Payload(String text) { this.text = text; }
    }

    static class OtherPayload extends Payload {
        OtherPayload(String text) { super(text); }
    }

    @JsonPropertyOrder({"a", "b"})
    static class TwoProperties {
        public Input a = new Input(new Payload("first"));
        public Input b = new Input(new Payload("second"));
    }

    static class NonEmptyProperty {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Input value = new Input(new Payload("present"));
    }

    static class CountingSerializer<T extends Payload> extends StdSerializer<T> {
        private final Class<T> type;
        private final Map<String, Integer> contextualizations;
        private final String propertyName;

        CountingSerializer(Class<T> type, Map<String, Integer> counts, String propertyName) {
            super(type);
            this.type = type;
            this.contextualizations = counts;
            this.propertyName = propertyName;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            String name = property == null ? "root" : property.getName();
            contextualizations.merge(type.getSimpleName() + ":" + name, 1, Integer::sum);
            return new CountingSerializer<>(type, contextualizations, name);
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(propertyName + ":" + value.text);
        }

        @Override
        public boolean isEmpty(SerializationContext ctxt, T value) {
            return value.text.isEmpty();
        }
    }

    private ObjectMapper mapper(Map<String, Integer> counts) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Payload.class, new CountingSerializer<>(Payload.class, counts, null));
        module.addSerializer(OtherPayload.class, new CountingSerializer<>(OtherPayload.class, counts, null));
        return JsonMapper.builder().addModule(module).build();
    }

    @Test
    void reuseEachDynamicOutputType() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        List<Input> values = List.of(new Input(new Payload("one")), new Input(new OtherPayload("two")),
                new Input(new Payload("three")), new Input(new OtherPayload("four")));
        assertEquals("[\"root:one\",\"root:two\",\"root:three\",\"root:four\"]",
                mapper.writeValueAsString(values));
        assertEquals(Map.of("Payload:root", 1, "OtherPayload:root", 1), counts);
    }

    @Test
    void keepPropertyContextSeparate() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        assertEquals("[{\"a\":\"a:first\",\"b\":\"b:second\"},{\"a\":\"a:first\",\"b\":\"b:second\"}]",
                mapper.writeValueAsString(List.of(new TwoProperties(), new TwoProperties())));
        assertEquals(Map.of("Payload:a", 1, "Payload:b", 1), counts);
    }

    @Test
    void reuseSerializerBetweenEmptinessCheckAndSerialization() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        assertEquals("{\"value\":\"value:present\"}", mapper.writeValueAsString(new NonEmptyProperty()));
        assertEquals(Map.of("Payload:value", 1), counts);
    }

    @Test
    void nullConversionDoesNotResolveSerializer() {
        Map<String, Integer> counts = new HashMap<>();
        assertEquals("[null,null]", mapper(counts).writeValueAsString(List.of(new Input(null), new Input(null))));
        assertTrue(counts.isEmpty());
    }
}

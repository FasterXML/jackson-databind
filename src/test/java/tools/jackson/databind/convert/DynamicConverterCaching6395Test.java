package tools.jackson.databind.convert;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdConvertingSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#6395]: dynamic converter output serializers should be reused within
// one write call without sharing contextualized serializers across calls.
class DynamicConverterCaching6395Test extends DatabindTestUtil
{
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

    @JsonPropertyOrder({ "a", "b" })
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
            contextualizations = counts;
            this.propertyName = propertyName;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            String name = (property == null) ? "root" : property.getName();
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

    @JsonSerialize(converter = DateInputConverter.class)
    static class DateInput {
        final Date value;

        DateInput(Date value) { this.value = value; }
    }

    static class DateInputConverter extends StdConverter<DateInput, Object> {
        @Override
        public Object convert(DateInput value) { return value.value; }
    }

    static class DateWrapper {
        @JsonFormat(pattern = "HH:mm")
        public DateInput when = new DateInput(new Date(0L));
    }

    static class SharedInput {
        final Payload value;

        SharedInput(String text) { value = new Payload(text); }
    }

    static class SharedConverter extends StdConverter<Object, Object> {
        @Override
        public Object convert(Object value) {
            return ((SharedInput) value).value;
        }
    }

    static class LabelSerializer extends StdSerializer<Payload> {
        private final String label;

        LabelSerializer(String label) {
            super(Payload.class);
            this.label = label;
        }

        @Override
        public void serialize(Payload value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(label + ":" + value.text);
        }
    }

    private ObjectMapper mapper(Map<String, Integer> counts) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Payload.class,
                new CountingSerializer<>(Payload.class, counts, null));
        module.addSerializer(OtherPayload.class,
                new CountingSerializer<>(OtherPayload.class, counts, null));
        return JsonMapper.builder().addModule(module).build();
    }

    @Test
    void reuseEachDynamicOutputTypeWithinWriteCall() {
        Map<String, Integer> counts = new HashMap<>();
        List<Input> values = List.of(
                new Input(new Payload("one")),
                new Input(new OtherPayload("two")),
                new Input(new Payload("three")),
                new Input(new OtherPayload("four")));

        assertEquals("[\"root:one\",\"root:two\",\"root:three\",\"root:four\"]",
                mapper(counts).writeValueAsString(values));
        assertEquals(Map.of("Payload:root", 1, "OtherPayload:root", 1), counts);
    }

    @Test
    void keepPropertyContextsSeparate() {
        Map<String, Integer> counts = new HashMap<>();

        assertEquals("[{\"a\":\"a:first\",\"b\":\"b:second\"},"
                        + "{\"a\":\"a:first\",\"b\":\"b:second\"}]",
                mapper(counts).writeValueAsString(
                        List.of(new TwoProperties(), new TwoProperties())));
        assertEquals(Map.of("Payload:a", 1, "Payload:b", 1), counts);
    }

    @Test
    void reuseSerializerBetweenEmptinessCheckAndSerialization() {
        Map<String, Integer> counts = new HashMap<>();

        assertEquals("{\"value\":\"value:present\"}",
                mapper(counts).writeValueAsString(new NonEmptyProperty()));
        assertEquals(Map.of("Payload:value", 1), counts);
    }

    @Test
    void contextualizeAgainForNewWriteCall() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        Input value = new Input(new Payload("value"));

        assertEquals("\"root:value\"", mapper.writeValueAsString(value));
        assertEquals("\"root:value\"", mapper.writeValueAsString(value));
        assertEquals(Map.of("Payload:root", 2), counts);
    }

    @Test
    void nullConversionDoesNotResolveSerializer() {
        Map<String, Integer> counts = new HashMap<>();

        assertEquals("[null,null]", mapper(counts).writeValueAsString(
                List.of(new Input(null), new Input(null))));
        assertTrue(counts.isEmpty());
    }

    @Test
    void preserveWriterSpecificTimeZone() {
        ObjectMapper mapper = newJsonMapper();
        ObjectWriter utc = mapper.writer().with(TimeZone.getTimeZone("UTC"));
        ObjectWriter tokyo = mapper.writer().with(TimeZone.getTimeZone("Asia/Tokyo"));

        assertEquals("{\"when\":\"00:00\"}", utc.writeValueAsString(new DateWrapper()));
        assertEquals("{\"when\":\"09:00\"}", tokyo.writeValueAsString(new DateWrapper()));
        assertEquals("{\"when\":\"00:00\"}", utc.writeValueAsString(new DateWrapper()));
    }

    @Test
    void keepSharedConverterSerializerMapperSpecific() {
        JavaType objectType = defaultTypeFactory().constructType(Object.class);
        StdConvertingSerializer shared = new StdConvertingSerializer(
                new SharedConverter(), objectType, null, null);

        ObjectMapper first = mapperWithSharedConverter(shared, "first");
        ObjectMapper second = mapperWithSharedConverter(shared, "second");

        assertEquals("\"first:value\"",
                first.writeValueAsString(new SharedInput("value")));
        assertEquals("\"second:value\"",
                second.writeValueAsString(new SharedInput("value")));
        assertEquals("\"first:value\"",
                first.writeValueAsString(new SharedInput("value")));
    }

    private ObjectMapper mapperWithSharedConverter(StdConvertingSerializer shared,
            String label) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(SharedInput.class, shared);
        module.addSerializer(Payload.class, new LabelSerializer(label));
        return JsonMapper.builder().addModule(module).build();
    }
}

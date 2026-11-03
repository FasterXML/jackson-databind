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
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicConverterCachingTest extends DatabindTestUtil {
    @JsonSerialize(converter = UntypedConverter.class)
    static class Input {
        final Object value;
        Input(Object value) { this.value = value; }
    }

    static class UntypedConverter extends StdConverter<Input, Object> {
        @Override
        public Object convert(Input value) { return value.value; }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonSerialize(converter = TypedConverter.class)
    static class TypedInput {
        final Object value;
        TypedInput(Object value) { this.value = value; }
    }

    static class TypedConverter extends StdConverter<TypedInput, Object> {
        @Override
        public Object convert(TypedInput value) { return value.value; }
    }

    static class DateToObjectConverter extends StdConverter<Date, Object> {
        @Override
        public Object convert(Date value) { return value; }
    }

    static class ConvertedDate {
        @JsonFormat(pattern = "HH:mm")
        @JsonSerialize(converter = DateToObjectConverter.class)
        public Date when = new Date(0L);
    }

    static class PlainDate {
        @JsonFormat(pattern = "HH:mm")
        public Date when = new Date(0L);
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
        public void serializeWithType(T value, JsonGenerator gen, SerializationContext ctxt,
                TypeSerializer typeSer) {
            WritableTypeId typeId = typeSer.writeTypePrefix(gen, ctxt,
                    typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, gen, ctxt);
            typeSer.writeTypeSuffix(gen, ctxt, typeId);
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
        return jsonMapperBuilder().addModule(module).build();
    }

    @Test
    void reuseEachDynamicOutputType() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        List<Input> values = List.of(new Input(new Payload("one")), new Input(new OtherPayload("two")),
                new Input(new Payload("three")), new Input(new OtherPayload("four")));
        assertEquals("""
                ["root:one","root:two","root:three","root:four"]""",
                mapper.writeValueAsString(values));
        assertEquals(Map.of("Payload:root", 1, "OtherPayload:root", 1), counts);
    }

    @Test
    void keepPropertyContextSeparate() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        assertEquals("""
                [{"a":"a:first","b":"b:second"},{"a":"a:first","b":"b:second"}]""",
                mapper.writeValueAsString(List.of(new TwoProperties(), new TwoProperties())));
        assertEquals(Map.of("Payload:a", 1, "Payload:b", 1), counts);
    }

    @Test
    void reuseSerializerBetweenEmptinessCheckAndSerialization() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        assertEquals("""
                {"value":"value:present"}""", mapper.writeValueAsString(new NonEmptyProperty()));
        assertEquals(Map.of("Payload:value", 1), counts);
    }

    @Test
    void nullConversionDoesNotResolveSerializer() {
        Map<String, Integer> counts = new HashMap<>();
        assertEquals("[null,null]", mapper(counts).writeValueAsString(List.of(new Input(null), new Input(null))));
        assertTrue(counts.isEmpty());
    }

    @Test
    void reuseDynamicOutputTypeWithPolymorphicValues() {
        Map<String, Integer> counts = new HashMap<>();
        ObjectMapper mapper = mapper(counts);
        TypedInput[] values = { new TypedInput(new Payload("one")), new TypedInput(new Payload("two")) };
        assertEquals("""
                [["Payload","root:one"],["Payload","root:two"]]""",
                mapper.writeValueAsString(values));
        assertEquals(Map.of("Payload:root", 1), counts);
    }

    // Serializer is contextualized once per mapper, so settings of the first
    // writer used stick: same limitation as with properties without converter.
    @Test
    void writerTimeZoneHandledSameAsWithoutConverter() {
        ObjectMapper mapper = newJsonMapper();
        ObjectWriter utc = mapper.writer().with(TimeZone.getTimeZone("UTC"));
        ObjectWriter tokyo = mapper.writer().with(TimeZone.getTimeZone("Asia/Tokyo"));
        for (ObjectWriter writer : List.of(utc, tokyo)) {
            assertEquals(writer.writeValueAsString(new PlainDate()),
                    writer.writeValueAsString(new ConvertedDate()));
        }
    }
}

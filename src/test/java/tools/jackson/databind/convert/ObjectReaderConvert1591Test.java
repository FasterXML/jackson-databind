package tools.jackson.databind.convert;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

class ObjectReaderConvert1591Test
{
    static class Bean {
        public String value;
        public int count;
    }

    static class AttributedSerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(ctxt.getAttribute("prefix") + value);
        }
    }

    static class AttributedDeserializer extends ValueDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString() + ctxt.getAttribute("suffix");
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void usesReaderAndWriterAttributes() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule()
                        .addSerializer(String.class, new AttributedSerializer())
                        .addDeserializer(String.class, new AttributedDeserializer()))
                .build();
        ObjectReader reader = mapper.readerFor(Bean.class).withAttribute("suffix", "!");
        ObjectWriter writer = mapper.writer().withAttribute("prefix", "hello ");

        Bean first = reader.convertValue(Map.of("value", "world"), writer);
        assertEquals("hello world!", first.value);
        Bean second = reader.withAttribute("suffix", "?")
                .convertValue(Map.of("value", "again"), writer.withAttribute("prefix", "hi "));
        assertEquals("hi again?", second.value);
        Bean repeated = reader.convertValue(Map.of("value", "world"), writer);
        assertEquals("hello world!", repeated.value);
    }

    @Test
    void usesConfiguredGenericTypeAndAcceptsNull() {
        ObjectReader reader = MAPPER.readerFor(new TypeReference<List<Bean>>() { });
        List<Bean> values = reader.convertValue(List.of(Map.of("value", "a", "count", 3)),
                MAPPER.writer());
        assertEquals(1, values.size());
        assertEquals("a", values.get(0).value);
        assertEquals(3, values.get(0).count);
        assertNull(reader.convertValue(null, MAPPER.writer()));
    }

    @Test
    void updatesExistingValue() {
        Bean existing = new Bean();
        existing.value = "kept";
        Bean result = MAPPER.readerForUpdating(existing)
                .convertValue(Map.of("count", 4), MAPPER.writer());
        assertSame(existing, result);
        assertEquals("kept", result.value);
        assertEquals(4, result.count);
    }

    @Test
    void honorsRootNamesOnReaderAndWriter() {
        Bean result = MAPPER.readerFor(Bean.class).withRootName("data")
                .convertValue(Map.of("value", "wrapped"), MAPPER.writer().withRootName("data"));
        assertEquals("wrapped", result.value);
    }

    @Test
    void rejectsMissingWriter() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> MAPPER.readerFor(Bean.class).convertValue(Map.of(), null));
        assertTrue(e.getMessage().contains("writer"));
    }
}

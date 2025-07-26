package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class StringArrayDeserializer5165Test
{
    static class Dst {
        public String[] array;
    }

    // Custom deserializer that converts empty strings to null
    static class EmptyStringToNullDeserializer extends StdDeserializer<String> {
        public EmptyStringToNullDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getValueAsString();
            if (value != null && value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    private ObjectMapper createMapperWithCustomDeserializer() {
        SimpleModule module = new SimpleModule()
            .addDeserializer(String.class, new EmptyStringToNullDeserializer());

        return JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = createMapperWithCustomDeserializer();

        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"array\":[\"\"]}", Dst.class)
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        SimpleModule module = new SimpleModule()
                .addDeserializer(String.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .changeDefaultNullHandling(n -> JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"array\":[\"\"]}", Dst.class);

        assertEquals(0, dst.array.length, "Null values should be skipped");
    }
}

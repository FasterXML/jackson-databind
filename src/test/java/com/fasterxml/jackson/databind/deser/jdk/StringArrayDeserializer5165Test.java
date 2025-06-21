package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class StringArrayDeserializer5165Test
{
    static class Dst {
        private String[] array;

        public String[] getArray() {
            return array;
        }

        public void setArray(String[] array) {
            this.array = array;
        }
    }

    // Custom deserializer that converts empty strings to null
    static class EmptyStringToNullDeserializer extends StdDeserializer<String> {
        private static final long serialVersionUID = 1L;

        public EmptyStringToNullDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value != null && value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    private ObjectMapper createMapperWithCustomDeserializer() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new EmptyStringToNullDeserializer());

        return JsonMapper.builder()
                .addModule(module)
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = createMapperWithCustomDeserializer();

        assertThrows(
                AssertionFailedError.class,
                () -> assertThrows(
                        InvalidNullException.class,
                        () -> mapper.readValue("{\"array\":[\"\"]}", new TypeReference<Dst>(){})
                ),
                "databind#5165 for StringArrayDeserializer is fixed"
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"array\":[\"\"]}", new TypeReference<Dst>() {});

        assertNotEquals(0, dst.getArray().length, "databind#5165 for StringArrayDeserializer is fixed");
    }
}

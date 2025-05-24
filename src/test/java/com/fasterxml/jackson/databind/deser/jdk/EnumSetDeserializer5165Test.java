package com.fasterxml.jackson.databind.deser.jdk;

import java.io.IOException;
import java.util.EnumSet;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class EnumSetDeserializer5165Test
{
    public enum MyEnum {
        FOO
    }

    static class Dst {
        private EnumSet<MyEnum> set;

        public EnumSet<MyEnum> getSet() {
            return set;
        }

        public void setSet(EnumSet<MyEnum> set) {
            this.set = set;
        }
    }

    // Custom deserializer that converts empty strings to null
    static class EmptyStringToNullDeserializer extends StdDeserializer<MyEnum> {
        private static final long serialVersionUID = 1L;

        public EmptyStringToNullDeserializer() {
            super(MyEnum.class);
        }

        @Override
        public MyEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value != null && value.isEmpty()) {
                return null;
            }
            return MyEnum.valueOf(value);
        }
    }

    private ObjectMapper createMapperWithCustomDeserializer() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MyEnum.class, new EmptyStringToNullDeserializer());

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
                        () -> mapper.readValue("{\"set\":[\"\"]}", new TypeReference<Dst>(){})
                ),
                "databind#5165 for EnumSetDeserializer is fixed"
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MyEnum.class, new EmptyStringToNullDeserializer());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"set\":[\"FOO\",\"\"]}", new TypeReference<Dst>() {});

        assertFalse(dst.getSet().isEmpty(), "databind#5165 for EnumSetDeserializer is fixed");
    }
}

package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class ObjectArrayDeserializer5165Test
{
    static class Dst {
        private Integer[] array;

        public Integer[] getArray() {
            return array;
        }

        public void setArray(Integer[] array) {
            this.array = array;
        }
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        assertThrows(
                AssertionFailedError.class,
                () -> assertThrows(
                        InvalidNullException.class,
                        () -> mapper.readValue("{\"array\":[\"\"]}", new TypeReference<Dst>(){})
                ),
                "databind#5165 for ObjectArrayDeserializer is fixed"
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"array\":[\"\"]}", new TypeReference<Dst>() {});

        assertNotEquals(0, dst.getArray().length, "databind#5165 for ObjectArrayDeserializer is fixed");
    }
}

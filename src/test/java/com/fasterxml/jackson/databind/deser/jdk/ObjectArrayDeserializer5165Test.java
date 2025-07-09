package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// For [databind#5165]
public class ObjectArrayDeserializer5165Test
{
    static class Dst {
        public Integer[] array;
    }

    @Test
    public void nullsFailTest() {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
                .build();

        // NOTE! Relies on default coercion of "" into `null` for `Integer`s...
        assertThrows(
                InvalidNullException.class,
                () -> mapper.readValue("{\"array\":[\"\"]}", Dst.class)
        );
    }

    @Test
    public void nullsSkipTest() throws Exception {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.SKIP))
                .build();

        Dst dst = mapper.readValue("{\"array\":[\"\"]}", Dst.class);
        // NOTE! Relies on default coercion of "" into `null` for `Integer`s...
        assertEquals(0, dst.array.length, "Null values should be skipped");
    }
}

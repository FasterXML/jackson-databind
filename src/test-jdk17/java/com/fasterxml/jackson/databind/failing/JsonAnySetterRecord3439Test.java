package com.fasterxml.jackson.databind.failing;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.BaseMapTest.newJsonMapper;

// [databind#3439]: Java Record @JsonAnySetter value is null after deserialization
public class JsonAnySetterRecord3439Test {
    record TestRecord(
        @JsonProperty String field,
        @JsonAnySetter Map<String, Object> anySetter
    ) {
    }

    @Test
    void testJsonAnySetterOnRecord() throws JsonProcessingException {
        // Given
        var json = """
            {
                "field": "value",
                "unmapped1": "value1",
                "unmapped2": "value2"
            }
            """;
        var objectMapper = newJsonMapper();
        TestRecord deserialized = null;

        // When
        try {
            // Behavior changed.
            // Jackson 2.13.2.2 returns null.
            // But Jackson 2.15 and later throws UnrecognizedPropertyException.
            deserialized = objectMapper.readValue(json, TestRecord.class);
        } catch (UnrecognizedPropertyException e) {
            throw e;
        }

        // Then
        assertEquals("value", deserialized.field());
        assertEquals(Map.of("unmapped1", "value1", "unmapped2", "value2"), deserialized.anySetter());
    }
}

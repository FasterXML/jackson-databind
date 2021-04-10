package com.fasterxml.jackson.failing;

import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// For [databind#2158]: The default map key deserializer bypasses `@JsonCreator` methods in favour
// of direct constructor invocation.
public class JsonCreatorMapKeyDeserialization2158Test extends BaseMapTest {
    private static final TypeReference<Map<DummyDto, Integer>> MAP_TYPE_REFERENCE =
            new TypeReference<Map<DummyDto, Integer>>() {};

    public void testDeserializeInvalidKey() {
        ObjectMapper mapper = new ObjectMapper();
        InvalidFormatException exception =
                assertThrows(
                        InvalidFormatException.class,
                        () -> mapper.readValue("{ \"\": 0 }", MAP_TYPE_REFERENCE));
        assertTrue(exception.getMessage().contains("Value must be nonempty"));
    }

    public void testNormalizeKey() {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(
                Collections.singletonMap(DummyDto.fromValue("foo"), 0),
                mapper.readValue("{ \"FOO\": 0 }", MAP_TYPE_REFERENCE));
    }

    private static final class DummyDto {
        @JsonValue
        private final String value;

        private DummyDto(String value) {
            this.value = value;
        }

        @JsonCreator
        static DummyDto fromValue(String value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Value must be nonempty");
            }

            return new DummyDto(value.toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DummyDto && ((DummyDto) o).value.equals(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return String.format("DummyDto{value=%s}", value);
        }
    }
}

package com.fasterxml.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4827] Subclassed Throwable deserialization fails since v2.18.0
//                no creator index for property 'cause'
public class SubclassedThrowableDeserialization4827Test
        extends DatabindTestUtil
{

    public static class SubclassedExceptionJava extends Exception {
        @JsonCreator
        public SubclassedExceptionJava(
                @JsonProperty("message") String message,
                @JsonProperty("cause") Throwable cause
        ) {
            super(message, cause);
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    @Test
    public void testDeserialization()
            throws Exception
    {
        // Given input
        SubclassedExceptionJava input = new SubclassedExceptionJava(
                "Test Message", new RuntimeException("test runtime cause"));

        // When serialize, then deserialize, round-trip
        String serialized = MAPPER.writeValueAsString(input);
        SubclassedExceptionJava deserialized = MAPPER.readValue(serialized, SubclassedExceptionJava.class);

        // Contents are same
        assertEquals(input.getMessage(), deserialized.getMessage());
        assertEquals(input.getCause().getMessage(), deserialized.getCause().getMessage());
    }
}

package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6394]: @JsonFormat(shape = ARRAY) deserialization
 * hangs when input ends before END_ARRAY while skipping extra values.
 * <p>
 * Verifies that deserialization throws MismatchedInputException instead
 * of hanging in an infinite loop when the parser reaches EOF.
 */
class ArrayDeser6394EofTest extends DatabindTestUtil
{

    @JsonFormat(shape = Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class P
    {
        public int x;
    }

    // Builder-based deserialization class for testing builder path
    @JsonDeserialize(builder = P.Builder.class)
    static class PWithBuilder
    {
        public int x;

        public static class Builder {
            private int x;

            public Builder withX(int x) {
                this.x = x;
                return this;
            }

            public PWithBuilder build() {
                return new PWithBuilder();
            }
        }
    }

    @Test
    void validArrayWithExtraScalarsDeserializesOk() throws Exception
    {
        // [1,2,3] with writeEndArray() should deserialize successfully
        // when FAIL_ON_UNKNOWN_PROPERTIES disabled
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            tb.writeNumber(3);
            tb.writeEndArray(); // Valid array must terminate properly

            try (JsonParser p = tb.asParser()) {
                P result = mapper.readerFor(P.class).readValue(p);
                assertNotNull(result);
                assertEquals(1, result.x);
            }
        }
    }

    @Test
    void validArrayWithExtraNestedObjectDeserializesOk() throws Exception
    {
        // [1,2,{"a":1}] with writeEndArray() should deserialize successfully
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            tb.writeStartObject();
            tb.writeStringProperty("a", "value");
            tb.writeEndObject();
            tb.writeEndArray(); // Valid array must terminate properly

            try (JsonParser p = tb.asParser()) {
                P result = mapper.readerFor(P.class).readValue(p);
                assertNotNull(result);
                assertEquals(1, result.x);
            }
        }
    }

    @Test
    void unterminatedArrayThrowsMismatchedInputException() throws Exception
    {
        // TokenBuffer without END_ARRAY must throw MismatchedInputException
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            // no writeEndArray() - this is the bug scenario

            try (JsonParser p = tb.asParser()) {
                MismatchedInputException e = assertThrows(MismatchedInputException.class,
                        () -> mapper.readerFor(P.class).readValue(p));
                // Use getOriginalMessage instead of fragile full getMessage() equality
                assertEquals("Unexpected end-of-input in array",
                        e.getOriginalMessage());
            }
        }
    }

    @Test
    void vanillaDeserializationHandlesEof() throws Exception
    {
        // Test the vanilla (non-builder) deserialization path
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            // no writeEndArray()

            try (JsonParser p = tb.asParser()) {
                MismatchedInputException e = assertThrows(MismatchedInputException.class,
                        () -> mapper.readerFor(P.class).readValue(p));
                // Use getOriginalMessage instead of fragile full getMessage() equality
                assertEquals("Unexpected end-of-input in array",
                        e.getOriginalMessage());
            }
        }
    }

    @Test
    void builderBasedDeserializationHandlesEof() throws Exception
    {
        // Test builder-based deserialization path using actual builder class
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            // no writeEndArray()

            try (JsonParser p = tb.asParser()) {
                MismatchedInputException e = assertThrows(MismatchedInputException.class,
                        () -> mapper.readerFor(PWithBuilder.class).readValue(p));
                // Use getOriginalMessage instead of fragile full getMessage() equality
                assertEquals("Unexpected end-of-input in array",
                        e.getOriginalMessage());
            }
        }
    }

    @Test
    void builderBasedDeserializationValidInput() throws Exception
    {
        // Test builder-based deserialization with valid terminated input
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            tb.writeEndArray(); // Valid terminated array

            try (JsonParser p = tb.asParser()) {
                PWithBuilder result = mapper.readerFor(PWithBuilder.class).readValue(p);
                assertNotNull(result);
                assertEquals(1, result.x);
            }
        }
    }

    // Helper method - no longer duplicated _numberAfterName/_assertRejected
}
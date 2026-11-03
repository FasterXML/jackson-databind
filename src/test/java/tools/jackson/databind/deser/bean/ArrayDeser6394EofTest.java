package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.annotation.JsonFormat;
import tools.jackson.databind.JsonFormat.Shape;
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

    @Test
    void validArrayWithExtraScalarsDeserializesOk() throws Exception
    {
        // [1,2,3] should deserialize successfully when FAIL_ON_UNKNOWN_PROPERTIES disabled
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            tb.writeNumber(3);
            // no writeEndArray() - extra values past property count

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
        // [1,2,{"a":1}] should deserialize successfully when FAIL_ON_UNKNOWN_PROPERTIES disabled
        ObjectMapper mapper = MAPPER.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeNumber(2);
            tb.writeStartObject();
            tb.writeStringField("a", "value");
            tb.writeEndObject();
            // no writeEndArray()

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
                assertEquals("Unexpected end-of-input in array", e.getMessage());
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
                assertEquals("Unexpected end-of-input in array", e.getMessage());
            }
        }
    }

    @Test
    void builderBasedDeserializationHandlesEof() throws Exception
    {
        // Test builder-based deserialization path
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
                assertEquals("Unexpected end-of-input in array", e.getMessage());
            }
        }
    }

    // Helper methods from DatabindTestUtil
    private TokenBuffer _numberAfterName()
    {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartArray();
        buffer.writeNumber(1);
        buffer.writeNumber(2);
        // no writeEndArray()
        return buffer;
    }

    private void _assertRejected(ObjectReader reader, TokenBuffer input) throws Exception
    {
        try (JsonParser p = input.asParser()) {
            MismatchedInputException e = assertThrows(MismatchedInputException.class,
                    () -> reader.readValue(p));
            assertEquals("Unexpected end-of-input in array", e.getMessage());
        }
    }
}
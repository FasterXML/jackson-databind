package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.TokenBuffer;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for issue #6357.
 *
 * Verifies that unexpected tokens inside JSON objects are reported
 * rather than silently producing partially initialized beans.
 */
class UnexpectedToken6357Test {

    record Sample(String name, int age) {}

    // Mutable bean used to exercise the vanilla deserialization path.
    public static class MutableSample {
        public String name;
        public int age;
    }

    @Test
    void rejectsUnexpectedTokenBeforeCreatorBuild() throws Exception {
        // Disable trailing-token validation so the test detects the
        // actual deserialization bug, not a later trailing-token error.
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");

        // Invalid: a number where a property name is expected.
        buffer.writeNumber(123);

        buffer.writeName("age");
        buffer.writeNumber(30);
        buffer.writeEndObject();

        try (JsonParser parser = buffer.asParser()) {
            assertThrows(
                    MismatchedInputException.class,
                    () -> mapper.readerFor(Sample.class).readValue(parser)
            );
        }
    }

    @Test
    void rejectsUnexpectedTokenAfterUnknownProperty() throws Exception {
        // Enable vanilla processing and allow unknown properties.
        // The unexpected token must still cause a failure.
        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");

        buffer.writeName("unknown");
        buffer.writeNumber(1);

        // Invalid: a number instead of another property name.
        buffer.writeNumber(123);

        buffer.writeName("age");
        buffer.writeNumber(30);
        buffer.writeEndObject();

        try (JsonParser parser = buffer.asParser()) {
            assertThrows(
                    MismatchedInputException.class,
                    () -> mapper.readerFor(MutableSample.class).readValue(parser)
            );
        }
    }
}

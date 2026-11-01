package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for [databind#6357]: unexpected token where a property name is expected
 * must be reported by property-based creator loops, not quietly end the Object.
 * Vanilla loops are covered by {@link BeanDeserializerVanillaTest}.
 */
class UnexpectedToken6357Test
{
    record Sample(String name, int age) {}

    public static class UnwrappedDetails {
        public String location;
    }

    public static class UnwrappedSample {
        @JsonUnwrapped
        public UnwrappedDetails details;

        @JsonCreator
        public UnwrappedSample(@JsonProperty("name") String name) { }
    }

    @JsonDeserialize(builder = BuiltSample.Builder.class)
    public static class BuiltSample {
        public static class Builder {
            @JsonCreator
            public Builder(@JsonProperty("name") String name,
                    @JsonProperty("category") String category) { }

            public BuiltSample build() {
                return new BuiltSample();
            }
        }
    }

    @JsonDeserialize(builder = UnwrappedBuiltSample.Builder.class)
    public static class UnwrappedBuiltSample {
        public static class Builder {
            @JsonUnwrapped
            public UnwrappedDetails details;

            @JsonCreator
            public Builder(@JsonProperty("name") String name,
                    @JsonProperty("category") String category) { }

            public UnwrappedBuiltSample build() {
                return new UnwrappedBuiltSample();
            }
        }
    }

    // Trailing-token check disabled so that it cannot be what reports the problem
    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    @Test
    void rejectsWithPropertyBasedCreator() throws Exception {
        _assertRejected(Sample.class);
    }

    @Test
    void rejectsWithUnwrappedPropertyBasedCreator() throws Exception {
        _assertRejected(UnwrappedSample.class);
    }

    @Test
    void rejectsWithPropertyBasedBuilder() throws Exception {
        _assertRejected(BuiltSample.class);
    }

    @Test
    void rejectsWithUnwrappedPropertyBasedBuilder() throws Exception {
        _assertRejected(UnwrappedBuiltSample.class);
    }

    // {"name":"Alice", 123, "category":"example"}: must fail at the bare number.
    // Second creator property only follows it, so the creator loop is still running.
    private void _assertRejected(Class<?> type) throws Exception {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");
        buffer.writeNumber(123);
        buffer.writeName("category");
        buffer.writeString("example");
        buffer.writeEndObject();

        try (JsonParser p = buffer.asParser()) {
            assertThrows(MismatchedInputException.class,
                    () -> MAPPER.readerFor(type).readValue(p));
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.currentToken());
        }
    }
}

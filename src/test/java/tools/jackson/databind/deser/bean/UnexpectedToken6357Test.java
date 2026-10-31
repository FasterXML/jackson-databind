package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.TokenBuffer;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    // Data used to ensure the @JsonUnwrapped code path is selected.
    public static class UnwrappedDetails {
        public String location;
    }

    public static class UnwrappedSample {
        public final String name;
        @JsonUnwrapped
        public UnwrappedDetails details;

        @JsonCreator
        public UnwrappedSample(@JsonProperty("name") String name) {
            this.name = name;
        }
    }

    @JsonDeserialize(builder = BuiltSample.Builder.class)
    public static class BuiltSample {
        public final String name;
        public final String category;

        BuiltSample(Builder builder) {
            name = builder.name;
            category = builder.category;
        }

        @JsonPOJOBuilder(withPrefix = "with")
        public static class Builder {
            public final String name;
            public final String category;

            @JsonCreator
            public Builder(@JsonProperty("name") String name,
                    @JsonProperty("category") String category) {
                this.name = name;
                this.category = category;
            }

            public BuiltSample build() {
                return new BuiltSample(this);
            }
        }
    }

    @JsonDeserialize(builder = UnwrappedBuiltSample.Builder.class)
    public static class UnwrappedBuiltSample {
        public final String name;
        public final String category;
        public final UnwrappedDetails details;

        UnwrappedBuiltSample(Builder builder) {
            name = builder.name;
            category = builder.category;
            details = builder.details;
        }

        @JsonPOJOBuilder(withPrefix = "with")
        public static class Builder {
            public final String name;
            public final String category;

            @JsonUnwrapped
            public UnwrappedDetails details;

            @JsonCreator
            public Builder(@JsonProperty("name") String name,
                    @JsonProperty("category") String category) {
                this.name = name;
                this.category = category;
            }

            public UnwrappedBuiltSample build() {
                return new UnwrappedBuiltSample(this);
            }
        }
    }

    private TokenBuffer malformedCreatorInput() {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");

        // This value is illegal in a field-name position.
        // We deliberately leave the second creator property absent so
        // builder code continues through its property-processing loop.
        buffer.writeNumber(123);
        buffer.writeName("category");
        buffer.writeString("example");
        buffer.writeEndObject();
        return buffer;
    }

    @Test
    void rejectsUnexpectedTokenWithUnwrappedPropertyCreator() throws Exception {
        // @JsonUnwrapped chooses BeanDeserializer's unwrapped creator loop.
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        try (JsonParser parser = malformedCreatorInput().asParser()) {
            assertThrows(MismatchedInputException.class,
                    () -> mapper.readerFor(UnwrappedSample.class).readValue(parser));
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.currentToken());
        }
    }

    @Test
    void rejectsUnexpectedTokenBeforeBuilderCreation() throws Exception {
        // Both builder constructor arguments must not be available when
        // the malformed token stops the property-based creator loop.
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        try (JsonParser parser = malformedCreatorInput().asParser()) {
            assertThrows(MismatchedInputException.class,
                    () -> mapper.readerFor(BuiltSample.class).readValue(parser));
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.currentToken());
        }
    }

    @Test
    void rejectsUnexpectedTokenBeforeUnwrappedBuilderCreation() throws Exception {
        // Combines a property-based builder with an unwrapped property,
        // exercising BuilderBasedDeserializer's unwrapped creator loop.
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        try (JsonParser parser = malformedCreatorInput().asParser()) {
            assertThrows(MismatchedInputException.class,
                    () -> mapper.readerFor(UnwrappedBuiltSample.class).readValue(parser));
            assertEquals(JsonToken.VALUE_NUMBER_INT, parser.currentToken());
        }
    }
}

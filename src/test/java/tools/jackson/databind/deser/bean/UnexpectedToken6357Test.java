package tools.jackson.databind.deser.bean;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for [databind#6357]: unexpected token where a property name is expected
 * must be reported, not quietly end the Object.
 * Vanilla loops are covered further by {@link BeanDeserializerVanillaTest}.
 */
class UnexpectedToken6357Test extends DatabindTestUtil
{
    record Sample(String name, int age) {}

    public static class MutableSample {
        public String name;
    }

    @JsonDeserialize(builder = DefaultBuiltSample.Builder.class)
    public static class DefaultBuiltSample {
        public static class Builder {
            public Builder withName(String name) {
                return this;
            }

            public DefaultBuiltSample build() {
                return new DefaultBuiltSample();
            }
        }
    }

    public static class ExternalValue { }

    public static class ExternalSample {
        @JsonCreator
        public ExternalSample(@JsonProperty("name") String name,
                @JsonProperty("value")
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
                @JsonSubTypes(@JsonSubTypes.Type(value = ExternalValue.class, name = "a"))
                ExternalValue value) { }
    }

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
    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    @Test
    void rejectsWithPropertyBasedCreator() throws Exception {
        _assertRejected(MAPPER.readerFor(Sample.class), _numberAfterName());
    }

    @Test
    void rejectsWithUnwrappedPropertyBasedCreator() throws Exception {
        _assertRejected(MAPPER.readerFor(UnwrappedSample.class), _numberAfterName());
    }

    @Test
    void rejectsWithPropertyBasedBuilder() throws Exception {
        _assertRejected(MAPPER.readerFor(BuiltSample.class), _numberAfterName());
    }

    @Test
    void rejectsWithUnwrappedPropertyBasedBuilder() throws Exception {
        _assertRejected(MAPPER.readerFor(UnwrappedBuiltSample.class), _numberAfterName());
    }

    @Test
    void rejectsWithExternalTypeIdCreator() throws Exception {
        _assertRejected(MAPPER.readerFor(ExternalSample.class), _numberAfterName());
    }

    @Test
    void rejectsRecordUpdate() throws Exception {
        _assertRejected(MAPPER.readerForUpdating(new Sample("Bob", 1)), _numberAfterName());
    }

    @Test
    void rejectsAsFirstTokenWithDefaultCreator() throws Exception {
        _assertRejected(MAPPER.readerFor(MutableSample.class), _numberFirst());
    }

    // Enabling view inclusion is what allows use of vanilla processing
    @Test
    void rejectsAsFirstTokenWithVanilla() throws Exception {
        ObjectMapper vanillaMapper = MAPPER.rebuild()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        _assertRejected(vanillaMapper.readerFor(MutableSample.class), _numberFirst());
    }

    @Test
    void rejectsAsFirstTokenWithBuilder() throws Exception {
        _assertRejected(MAPPER.readerFor(DefaultBuiltSample.class), _numberFirst());
    }

    @Test
    void rejectsAsFirstTokenWithRecordUpdate() throws Exception {
        _assertRejected(MAPPER.readerForUpdating(new Sample("Bob", 1)), _numberFirst());
    }

    @Test
    void rejectsAsFirstTokenWithPojoUpdate() throws Exception {
        _assertRejected(MAPPER.readerForUpdating(new MutableSample()), _numberFirst());
    }

    // Not a problem with token inside Object, so has its own message
    @Test
    void endOfInputWithinObjectReported() throws Exception {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");

        try (JsonParser p = buffer.asParser()) {
            MismatchedInputException e = assertThrows(MismatchedInputException.class,
                    () -> MAPPER.readerFor(Sample.class).readValue(p));
            verifyException(e, "Unexpected end-of-input");
        }
    }

    // {"name":"Alice", 123, "category":"example"}: second creator property only
    // follows the bare number, so the creator loop is still running when it is reached
    private TokenBuffer _numberAfterName() {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeName("name");
        buffer.writeString("Alice");
        buffer.writeNumber(123);
        buffer.writeName("category");
        buffer.writeString("example");
        buffer.writeEndObject();
        return buffer;
    }

    // {123, "name":"Alice"}
    private TokenBuffer _numberFirst() {
        TokenBuffer buffer = TokenBuffer.forGeneration();
        buffer.writeStartObject();
        buffer.writeNumber(123);
        buffer.writeName("name");
        buffer.writeString("Alice");
        buffer.writeEndObject();
        return buffer;
    }

    // Must fail at the bare number
    private void _assertRejected(ObjectReader reader, TokenBuffer input) throws Exception {
        try (JsonParser p = input.asParser()) {
            MismatchedInputException e = assertThrows(MismatchedInputException.class,
                    () -> reader.readValue(p));
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.currentToken());
            verifyException(e, "Unexpected token (`JsonToken.VALUE_NUMBER_INT`) within Object value");
            verifyException(e, "expected property name or `JsonToken.END_OBJECT`");
        }
    }
}

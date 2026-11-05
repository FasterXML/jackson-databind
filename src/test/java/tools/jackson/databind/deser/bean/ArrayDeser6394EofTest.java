package tools.jackson.databind.deser.bean;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6394]: {@code @JsonFormat(shape = ARRAY)} deserialization
 * hung when input ended before END_ARRAY while skipping extra values
 * (with {@code FAIL_ON_UNKNOWN_PROPERTIES} disabled).
 */
class ArrayDeser6394EofTest extends DatabindTestUtil
{
    static class ActiveView { }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class Pojo {
        @JsonView(ActiveView.class)
        public int x;
    }

    static class CreatorPojo extends Pojo {
        @JsonCreator
        CreatorPojo(@JsonProperty("x") int x) {
            this.x = x;
        }
    }

    // Value types acting as their own Builders, to avoid separate Builder classes
    @JsonDeserialize(builder = BuiltPojo.class)
    static class BuiltPojo extends Pojo {
        public BuiltPojo build() {
            return this;
        }
    }

    @JsonDeserialize(builder = BuiltCreatorPojo.class)
    static class BuiltCreatorPojo extends CreatorPojo {
        @JsonCreator
        BuiltCreatorPojo(@JsonProperty("x") int x) {
            super(x);
        }

        public BuiltCreatorPojo build() {
            return this;
        }
    }

    private static final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // One reader per code path: an active view prevents "vanilla" processing
    static Stream<ObjectReader> readers() {
        return Stream.of(
                MAPPER.readerFor(Pojo.class),
                MAPPER.readerFor(Pojo.class).withView(ActiveView.class),
                MAPPER.readerForUpdating(new Pojo()),
                MAPPER.readerFor(CreatorPojo.class),
                MAPPER.readerFor(BuiltPojo.class),
                MAPPER.readerFor(BuiltPojo.class).withView(ActiveView.class),
                MAPPER.readerFor(BuiltCreatorPojo.class));
    }

    @ParameterizedTest
    @MethodSource("readers")
    void unterminatedArrayFails(ObjectReader r) throws Exception {
        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            tb.writeStartObject();
            tb.writeEndObject();
            tb.writeNumber(2);
            // no END_ARRAY: the bug scenario

            try (JsonParser p = tb.asParser()) {
                // Preemptive, so that a regression fails the test instead of hanging the build
                MismatchedInputException e = assertTimeoutPreemptively(Duration.ofSeconds(10),
                        () -> assertThrows(MismatchedInputException.class,
                                () -> r.readValue(p)));
                verifyException(e, "Unexpected end-of-input in array");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("readers")
    void extraValuesSkipped(ObjectReader r) throws Exception {
        Pojo result = r.readValue("""
                [1,{"a":[1,2]},3]
                """);
        assertEquals(1, result.x);
    }
}

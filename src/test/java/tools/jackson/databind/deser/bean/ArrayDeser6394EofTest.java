package tools.jackson.databind.deser.bean;

import java.time.Duration;

import org.junit.jupiter.api.Test;

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
        public int x;
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class PojoWithView {
        @JsonView(ActiveView.class)
        public int x;
    }

    static class Value {
        final int x;

        Value(int x) {
            this.x = x;
        }
    }

    @JsonDeserialize(builder = ValueBuilder.class)
    static class BuiltValue extends Value {
        BuiltValue(int x) {
            super(x);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class ValueBuilder {
        private int x;

        public ValueBuilder withX(int v) {
            x = v;
            return this;
        }

        public BuiltValue build() {
            return new BuiltValue(x);
        }
    }

    @JsonDeserialize(builder = ViewValueBuilder.class)
    static class BuiltViewValue extends Value {
        BuiltViewValue(int x) {
            super(x);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class ViewValueBuilder {
        private int x;

        @JsonView(ActiveView.class)
        public ViewValueBuilder withX(int v) {
            x = v;
            return this;
        }

        public BuiltViewValue build() {
            return new BuiltViewValue(x);
        }
    }

    @JsonDeserialize(builder = CreatorValueBuilder.class)
    static class BuiltCreatorValue extends Value {
        BuiltCreatorValue(int x) {
            super(x);
        }
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "x" })
    static class CreatorValueBuilder {
        private final int x;

        @JsonCreator
        CreatorValueBuilder(@JsonProperty("x") int x) {
            this.x = x;
        }

        public BuiltCreatorValue build() {
            return new BuiltCreatorValue(x);
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /*
    /**********************************************************************
    /* Test methods: unterminated input must fail, not hang
    /**********************************************************************
     */

    @Test
    void pojoVanillaEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerFor(Pojo.class));
    }

    @Test
    void pojoNonVanillaEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerFor(PojoWithView.class).withView(ActiveView.class));
    }

    @Test
    void pojoUpdateEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerForUpdating(new Pojo()));
    }

    @Test
    void builderVanillaEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerFor(BuiltValue.class));
    }

    @Test
    void builderNonVanillaEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerFor(BuiltViewValue.class).withView(ActiveView.class));
    }

    @Test
    void builderPropertyBasedEof() throws Exception {
        _assertFailsOnEof(MAPPER.readerFor(BuiltCreatorValue.class));
    }

    /*
    /**********************************************************************
    /* Test methods: terminated input with extra values still works
    /**********************************************************************
     */

    @Test
    void pojoSkipsExtraValues() throws Exception {
        assertEquals(1, MAPPER.readValue("[1,2,3]", Pojo.class).x);
        assertEquals(1, MAPPER.readValue("""
                [1,{"a":[1,2]},3]
                """, Pojo.class).x);
    }

    @Test
    void builderSkipsExtraValues() throws Exception {
        assertEquals(1, MAPPER.readValue("[1,2,3]", BuiltValue.class).x);
        assertEquals(1, MAPPER.readValue("""
                [1,{"a":[1,2]},3]
                """, BuiltValue.class).x);
    }

    @Test
    void builderNonVanillaSkipsExtraValues() throws Exception {
        BuiltViewValue value = MAPPER.readerFor(BuiltViewValue.class)
                .withView(ActiveView.class)
                .readValue("""
                [1,{"a":[1,2]},3]
                """);
        assertEquals(1, value.x);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void _assertFailsOnEof(ObjectReader r) throws Exception {
        _assertFailsOnEof(r, false);
        _assertFailsOnEof(r, true);
    }

    private void _assertFailsOnEof(ObjectReader r, boolean structuredExtra) throws Exception {
        try (TokenBuffer tb = TokenBuffer.forGeneration()) {
            tb.writeStartArray();
            tb.writeNumber(1);
            if (structuredExtra) {
                tb.writeStartObject();
                tb.writeEndObject();
            } else {
                tb.writeNumber(2);
            }
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
}

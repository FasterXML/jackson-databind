package tools.jackson.databind.deser.builder;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class BuilderArrayExtraValues6374Test extends DatabindTestUtil
{
    private static final String OBJECT_EXTRA = """
            {"z":[3]}""";

    static class View { }

    @JsonDeserialize(builder = Value.Builder.class)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    record Value(int x, int y) {
        @JsonPropertyOrder({ "x", "y" })
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        static class Builder {
            private int x, y;

            @JsonView(View.class)
            public Builder withX(int v) { x = v; return this; }
            @JsonView(View.class)
            public Builder withY(int v) { y = v; return this; }
            public Value build() { return new Value(x, y); }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class IgnoringBuilder { }

    static class Wrapper {
        @JsonView(View.class)
        public Value value;
        @JsonView(View.class)
        public int after;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper IGNORING_MAPPER = jsonMapperBuilder()
            .addMixIn(Value.Builder.class, IgnoringBuilder.class)
            .build();

    // Without active View, vanilla processing is used; with one, non-vanilla
    private ObjectReader reader(ObjectMapper mapper, boolean view) {
        return view ? mapper.readerWithView(View.class) : mapper.reader();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void skipsExtraValuesAtRoot(boolean view) {
        ObjectReader reader = reader(MAPPER, view).forType(Value.class);
        for (String extra : new String[] { "[3]", "[]", OBJECT_EXTRA, "{}", "3", "3,[4]" }) {
            assertEquals(new Value(1, 2), reader.readValue("[1,2," + extra + "]"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void leavesParserAtEndOfNestedValue(boolean view) {
        ObjectReader reader = reader(MAPPER, view);
        for (String extra : new String[] { "[3]", OBJECT_EXTRA }) {
            List<Value> values = reader.forType(new TypeReference<List<Value>>() { })
                    .readValue("[[1,2," + extra + "],[4,5]]");
            assertEquals(List.of(new Value(1, 2), new Value(4, 5)), values);

            Wrapper wrapper = reader.forType(Wrapper.class)
                    .readValue("""
                            {"value":[1,2,%s],"after":7}""".formatted(extra));
            assertEquals(new Value(1, 2), wrapper.value);
            assertEquals(7, wrapper.after);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void honorsIgnoreUnknownAnnotation(boolean view) {
        ObjectReader reader = reader(IGNORING_MAPPER, view)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .forType(Value.class);
        assertEquals(new Value(1, 2), reader.readValue("[1,2,[3]]"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void stillRejectsExtraValuesWhenStrict(boolean view) {
        ObjectReader reader = reader(MAPPER, view)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .forType(Value.class);
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> reader.readValue("[1,2,[3]]"));
        verifyException(e, "expected at most 2 properties");
    }
}

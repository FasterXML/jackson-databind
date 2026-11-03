package tools.jackson.databind.deser.builder;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class BuilderArrayExtraValues6374Test
{
    @JsonDeserialize(builder = Value.Builder.class)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class Value {
        final int x, y;

        Value(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @JsonPropertyOrder({ "x", "y" })
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        static class Builder {
            private int x, y;

            public Builder withX(int v) { x = v; return this; }
            public Builder withY(int v) { y = v; return this; }
            public Value build() { return new Value(x, y); }
        }
    }

    static class View { }

    abstract static class ViewedBuilder {
        @JsonView(View.class)
        public abstract Value.Builder withX(int v);
        @JsonView(View.class)
        public abstract Value.Builder withY(int v);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class IgnoringBuilder { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class IgnoringViewedBuilder extends ViewedBuilder { }

    static class Wrapper {
        @JsonView(View.class)
        public Value value;
        @JsonView(View.class)
        public int after;
    }

    private ObjectMapper mapper(boolean view, boolean ignoreByAnnotation) {
        JsonMapper.Builder builder = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        if (ignoreByAnnotation) {
            builder.addMixIn(Value.Builder.class,
                    view ? IgnoringViewedBuilder.class : IgnoringBuilder.class);
        } else if (view) {
            builder.addMixIn(Value.Builder.class, ViewedBuilder.class);
        }
        // Without active Views, enable default inclusion to use vanilla processing path
        if (!view) {
            builder.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        }
        return builder.build();
    }

    private ObjectReader reader(boolean view, boolean ignoreByAnnotation) {
        ObjectReader reader = mapper(view, ignoreByAnnotation).reader();
        return view ? reader.withView(View.class) : reader;
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void skipsExtraValuesAtRoot(boolean view) {
        ObjectReader reader = reader(view, false).forType(Value.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        for (String extra : new String[] { "[3]", "[]", "{\"z\":[3]}", "{}", "3", "3,[4]" }) {
            Value value = reader.readValue("[1,2," + extra + "]");
            assertEquals(1, value.x);
            assertEquals(2, value.y);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void leavesParserAtEndOfNestedValue(boolean view) {
        ObjectReader reader = reader(view, false)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        for (String extra : new String[] { "[3]", "{\"z\":[3]}" }) {
            List<Value> values = reader.forType(new TypeReference<List<Value>>() { })
                    .readValue("[[1,2," + extra + "],[4,5]]");
            assertEquals(2, values.size());
            assertEquals(1, values.get(0).x);
            assertEquals(2, values.get(0).y);
            assertEquals(4, values.get(1).x);
            assertEquals(5, values.get(1).y);

            Wrapper wrapper = reader.forType(Wrapper.class)
                    .readValue("{\"value\":[1,2," + extra + "],\"after\":7}");
            assertEquals(1, wrapper.value.x);
            assertEquals(2, wrapper.value.y);
            assertEquals(7, wrapper.after);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void honorsIgnoreUnknownAnnotation(boolean view) {
        Value value = reader(view, true).forType(Value.class).readValue("[1,2,[3]]");
        assertEquals(1, value.x);
        assertEquals(2, value.y);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void stillRejectsExtraValuesWhenStrict(boolean view) {
        ObjectReader reader = reader(view, false).forType(Value.class);
        assertThrows(MismatchedInputException.class, () -> reader.readValue("[1,2,[3]]"));
    }
}

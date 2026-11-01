package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayBuilderWithView6363Test
{
    static class ViewX { }
    static class ViewXY extends ViewX { }

    @JsonDeserialize(builder = Value.Builder.class)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class Value {
        final int x;
        final int y;

        Value(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @JsonPropertyOrder({ "x", "y" })
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        static class Builder {
            private final int x;
            private final int y;

            Builder() {
                this(0, 0);
            }

            Builder(int x, int y) {
                this.x = x;
                this.y = y;
            }

            @JsonView(ViewX.class)
            public Builder withX(int value) {
                return new Builder(value, y);
            }

            @JsonView(ViewXY.class)
            public Builder withY(int value) {
                return new Builder(x, value);
            }

            public Value build() {
                return new Value(x, y);
            }
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    @Test
    void retainsBothValuesWithActiveView() throws Exception {
        Value result = MAPPER.readerWithView(ViewXY.class)
                .forType(Value.class).readValue("[1,2]");

        assertEquals(1, result.x);
        assertEquals(2, result.y);
    }

    @Test
    void retainsVisibleValueWhenOtherPropertyIsFiltered() throws Exception {
        Value result = MAPPER.readerWithView(ViewX.class)
                .forType(Value.class).readValue("[1,2]");

        assertEquals(1, result.x);
        assertEquals(0, result.y);
    }

    @Test
    void retainsValuesWithoutActiveView() throws Exception {
        Value result = MAPPER.readValue("[1,2]", Value.class);

        assertEquals(1, result.x);
        assertEquals(2, result.y);
    }
}

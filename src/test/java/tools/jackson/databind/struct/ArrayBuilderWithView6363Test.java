package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArrayBuilderWithView6363Test extends DatabindTestUtil
{
    // Immutable builder with Object Id, as Array
    @JsonDeserialize(builder = ArrayIdValue.Builder.class)
    static class ArrayIdValue {
        final Integer id;
        final int x;

        ArrayIdValue(Integer id, int x) {
            this.id = id;
            this.x = x;
        }

        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        @JsonPropertyOrder({ "id", "x" })
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        static class Builder {
            private final Integer id;
            private final int x;

            Builder() {
                this(null, 0);
            }

            Builder(Integer id, int x) {
                this.id = id;
                this.x = x;
            }

            public Builder withId(Integer value) {
                return new Builder(value, x);
            }

            public Builder withX(int value) {
                return new Builder(id, value);
            }

            public ArrayIdValue build() {
                return new ArrayIdValue(id, x);
            }
        }
    }

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

    private final ObjectMapper MAPPER = jsonMapperBuilder()
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

    @Test
    void retainsValuesWithObjectId() throws Exception {
        ArrayIdValue result = MAPPER.readValue("[1,5]", ArrayIdValue.class);

        assertEquals(Integer.valueOf(1), result.id);
        assertEquals(5, result.x);
    }

    // Null Object Id is legal, see [databind#742]: must not lose the builder
    @Test
    void retainsBuilderWithNullObjectId() throws Exception {
        ArrayIdValue result = MAPPER.readValue("[null,5]", ArrayIdValue.class);

        assertNull(result.id);
        assertEquals(5, result.x);
    }

    @Test
    void retainsBuilderWithNullObjectIdOnly() throws Exception {
        ArrayIdValue result = MAPPER.readValue("[null]", ArrayIdValue.class);

        assertNull(result.id);
        assertEquals(0, result.x);
    }
}

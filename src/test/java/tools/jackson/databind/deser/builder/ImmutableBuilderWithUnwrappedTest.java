package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableBuilderWithUnwrappedTest extends DatabindTestUtil
{
    static class PublicView { }
    static class InternalView extends PublicView { }

    static class Point {
        @JsonView(PublicView.class)
        public int x;
    }

    @JsonDeserialize(builder = Value.Builder.class)
    static class Value {
        final int id;
        final Point left;
        final Point right;

        Value(Builder builder) {
            id = builder.id;
            left = builder.left;
            right = builder.right;
        }

        static class Builder {
            final int id;
            final Point left;
            final Point right;

            Builder() {
                this(0, null, null);
            }

            Builder(int id, Point left, Point right) {
                this.id = id;
                this.left = left;
                this.right = right;
            }

            @JsonView(PublicView.class)
            public Builder withId(int value) {
                return new Builder(value, left, right);
            }

            @JsonUnwrapped(prefix = "left.")
            @JsonView(PublicView.class)
            public Builder withLeft(Point value) {
                return new Builder(id, value, right);
            }

            @JsonUnwrapped(prefix = "right.")
            @JsonView(InternalView.class)
            public Builder withRight(Point value) {
                return new Builder(id, left, value);
            }

            public Value build() {
                return new Value(this);
            }
        }
    }

    @JsonDeserialize(builder = CreatorBuilder.class)
    static class CreatorValue extends Value {
        CreatorValue(Value.Builder builder) {
            super(builder);
        }
    }

    static class CreatorBuilder extends Value.Builder {
        @JsonCreator
        CreatorBuilder(@JsonProperty("id") int id) {
            this(id, null, null);
        }

        CreatorBuilder(int id, Point left, Point right) {
            super(id, left, right);
        }

        @Override
        @JsonUnwrapped(prefix = "left.")
        public CreatorBuilder withLeft(Point value) {
            return new CreatorBuilder(id, value, right);
        }

        @Override
        @JsonUnwrapped(prefix = "right.")
        public CreatorBuilder withRight(Point value) {
            return new CreatorBuilder(id, left, value);
        }

        @Override
        public CreatorValue build() {
            return new CreatorValue(this);
        }
    }

    static class MutableValue {
        Point point;

        @JsonUnwrapped
        public int setPoint(Point value) {
            point = value;
            return 42;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    @Test
    void defaultBuilderRetainsBothUnwrappedValues() throws Exception {
        Value result = MAPPER.readValue("""
                {"id":7,"left.x":1,"right.x":2}
                """, Value.class);

        assertValues(result);
    }

    @Test
    void creatorBuilderRetainsBothUnwrappedValues() throws Exception {
        CreatorValue result = MAPPER.readValue(
                """
                {"id":7,"left.x":1,"right.x":2}
                """, CreatorValue.class);

        assertValues(result);
    }

    @Test
    void creatorBuilderRetainsBufferedUnwrappedValues() throws Exception {
        CreatorValue result = MAPPER.readValue(
                """
                {"left.x":1,"right.x":2,"id":7}
                """, CreatorValue.class);

        assertValues(result);
    }

    @Test
    void viewFilteringIsPreserved() throws Exception {
        Value result = MAPPER.readerWithView(PublicView.class).forType(Value.class)
                .readValue("""
                {"id":7,"left.x":1,"right.x":2}
                """);

        assertEquals(7, result.id);
        assertNotNull(result.left);
        assertEquals(1, result.left.x);
        assertNull(result.right);
    }

    @Test
    void absentUnwrappedValuesStayNull() throws Exception {
        Value result = MAPPER.readValue("""
                {"id":7}
                """, Value.class);

        assertEquals(7, result.id);
        assertNull(result.left);
        assertNull(result.right);

        CreatorValue creatorResult = MAPPER.readValue("""
                {"id":7}
                """, CreatorValue.class);
        assertEquals(7, creatorResult.id);
        assertNull(creatorResult.left);
        assertNull(creatorResult.right);
    }

    @Test
    void mutableBeanSetterReturnValueRemainsIgnored() throws Exception {
        MutableValue result = MAPPER.readValue("""
                {"x":1}
                """, MutableValue.class);

        assertNotNull(result.point);
        assertEquals(1, result.point.x);
    }

    private void assertValues(Value result) {
        assertEquals(7, result.id);
        assertNotNull(result.left);
        assertEquals(1, result.left.x);
        assertNotNull(result.right);
        assertEquals(2, result.right.x);
    }
}

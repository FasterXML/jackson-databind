package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImmutableBuilderWithCreator6397Test extends DatabindTestUtil
{
    @JsonDeserialize(builder = ImmutableBuilder.class)
    record Value(int a, int b, int c, int d) { }

    @JsonPropertyOrder({ "a", "b", "c", "d" })
    static class ImmutableBuilder
    {
        private final int a, b, c, d;

        @JsonCreator
        ImmutableBuilder(@JsonProperty("c") int c) {
            this(0, 0, c, 0);
        }

        private ImmutableBuilder(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public ImmutableBuilder withA(int value) {
            return new ImmutableBuilder(value, b, c, d);
        }

        public ImmutableBuilder withB(int value) {
            return new ImmutableBuilder(a, value, c, d);
        }

        public ImmutableBuilder withD(int value) {
            return new ImmutableBuilder(a, b, c, value);
        }

        public Value build() {
            return new Value(a, b, c, d);
        }
    }

    static class Point {
        public int x;
    }

    @JsonDeserialize(builder = UnwrappedBuilder.class)
    record UnwrappedValue(int a, int c, int x) { }

    static class UnwrappedBuilder
    {
        private final int a, c;
        private final Point point;

        @JsonCreator
        UnwrappedBuilder(@JsonProperty("c") int c) {
            this(0, c, null);
        }

        private UnwrappedBuilder(int a, int c, Point point) {
            this.a = a;
            this.c = c;
            this.point = point;
        }

        public UnwrappedBuilder withA(int value) {
            return new UnwrappedBuilder(value, c, point);
        }

        @JsonUnwrapped(prefix = "p.")
        public UnwrappedBuilder withPoint(Point value) {
            return new UnwrappedBuilder(a, c, value);
        }

        public UnwrappedValue build() {
            return new UnwrappedValue(a, c, (point == null) ? -1 : point.x);
        }
    }

    static class MutableBean
    {
        final int id;
        String name;

        @JsonCreator
        MutableBean(@JsonProperty("id") int id) {
            this.id = id;
        }

        public String setName(String name) {
            this.name = name;
            return name;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper ARRAY_MAPPER = jsonMapperBuilder()
            .withConfigOverride(ImmutableBuilder.class,
                    o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.ARRAY)))
            .build();

    // Creator property completes mid-object
    @Test
    void retainsBufferedObjectBuilderProperties()
    {
        assertEquals(new Value(5, 6, 7, 8), MAPPER.readValue("""
                {"a":5,"b":6,"c":7,"d":8}
                """, Value.class));
    }

    // Creator property completes as last property, right before END_OBJECT
    @Test
    void retainsBufferedObjectBuilderPropertiesCreatorLast()
    {
        assertEquals(new Value(5, 6, 7, 8), MAPPER.readValue("""
                {"a":5,"b":6,"d":8,"c":7}
                """, Value.class));
    }

    // Creator property missing: build happens at END_OBJECT
    @Test
    void retainsBufferedObjectBuilderPropertiesCreatorMissing()
    {
        assertEquals(new Value(5, 6, 0, 8), MAPPER.readValue("""
                {"a":5,"b":6,"d":8}
                """, Value.class));
    }

    // Creator property completes mid-array
    @Test
    void retainsBufferedArrayBuilderProperties()
    {
        assertEquals(new Value(5, 6, 7, 8), ARRAY_MAPPER.readValue("""
                [5,6,7,8]
                """, Value.class));
    }

    // Creator property missing: build happens at END_ARRAY
    @Test
    void retainsBufferedArrayBuilderPropertiesCreatorMissing()
    {
        assertEquals(new Value(5, 6, 0, 0), ARRAY_MAPPER.readValue("""
                [5,6]
                """, Value.class));
    }

    // With unwrapped property: creator property completes mid-object
    @Test
    void retainsBufferedUnwrappedBuilderProperties()
    {
        assertEquals(new UnwrappedValue(5, 7, 3), MAPPER.readValue("""
                {"a":5,"p.x":3,"c":7}
                """, UnwrappedValue.class));
    }

    // With unwrapped property: creator property missing, build at END_OBJECT
    @Test
    void retainsBufferedUnwrappedBuilderPropertiesCreatorMissing()
    {
        assertEquals(new UnwrappedValue(5, 0, 3), MAPPER.readValue("""
                {"a":5,"p.x":3}
                """, UnwrappedValue.class));
    }

    @Test
    void ignoresSetterReturnValueForOrdinaryBean()
    {
        MutableBean value = MAPPER.readValue("""
                {"name":"value","id":7}
                """, MutableBean.class);

        assertEquals(7, value.id);
        assertEquals("value", value.name);
    }
}

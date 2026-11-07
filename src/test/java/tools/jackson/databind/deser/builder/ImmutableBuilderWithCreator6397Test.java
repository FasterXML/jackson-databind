package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImmutableBuilderWithCreator6397Test
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

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void retainsBufferedArrayBuilderProperties()
    {
        ObjectMapper mapper = JsonMapper.builder()
                .withConfigOverride(ImmutableBuilder.class,
                        o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.ARRAY)))
                .build();

        assertEquals(new Value(5, 6, 7, 8), mapper.readValue("""
                [5,6,7,8]
                """, Value.class));
    }

    @Test
    void retainsBufferedObjectBuilderProperties()
    {
        assertEquals(new Value(5, 6, 7, 8), MAPPER.readValue("""
                {"a":5,"b":6,"c":7,"d":8}
                """, Value.class));
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

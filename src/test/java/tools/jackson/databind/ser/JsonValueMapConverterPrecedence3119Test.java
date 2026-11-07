package tools.jackson.databind.ser;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.util.StdConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueMapConverterPrecedence3119Test
{
    public static class AccessorString extends StdConverter<Integer, String> {
        @Override
        public String convert(Integer value) {
            return "accessor-" + value;
        }
    }

    public static class EnclosingString extends StdConverter<Integer, String> {
        @Override
        public String convert(Integer value) {
            return "enclosing-" + value;
        }
    }

    public static class EnclosingLong extends StdConverter<Integer, Long> {
        @Override
        public Long convert(Integer value) {
            return value.longValue() + 100;
        }
    }

    public static class StringContent extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("S:" + value);
        }
    }

    public static class LongContent extends ValueSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("L:" + value);
        }
    }

    public static class WithAccessorConverter {
        @JsonValue
        @JsonSerialize(contentConverter = AccessorString.class)
        public Map<String, Integer> values() {
            return Map.of("a", 7);
        }
    }

    public static class PlainWrapper {
        @JsonValue
        public Map<String, Integer> values() {
            return Map.of("a", 7);
        }
    }

    public static class SameOutputType {
        @JsonSerialize(contentConverter = EnclosingString.class, contentUsing = StringContent.class)
        public WithAccessorConverter value = new WithAccessorConverter();
    }

    public static class DifferentOutputType {
        @JsonSerialize(contentConverter = EnclosingLong.class, contentUsing = LongContent.class)
        public WithAccessorConverter value = new WithAccessorConverter();
    }

    public static class NoAccessorConverter {
        @JsonSerialize(contentConverter = EnclosingString.class, contentUsing = StringContent.class)
        public PlainWrapper value = new PlainWrapper();
    }

    public static class DirectStringMap {
        @JsonSerialize(contentConverter = EnclosingString.class, contentUsing = StringContent.class)
        public Map<String, Integer> value = Map.of("a", 7);
    }

    public static class DirectLongMap {
        @JsonSerialize(contentConverter = EnclosingLong.class, contentUsing = LongContent.class)
        public Map<String, Integer> value = Map.of("a", 7);
    }

    private ObjectMapper mapper(boolean staticTyping) {
        return staticTyping ? JsonMapper.builder().enable(MapperFeature.USE_STATIC_TYPING).build()
                : JsonMapper.builder().build();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void enclosingConverterTakesPrecedenceWithSameOutputType(boolean staticTyping) {
        ObjectMapper mapper = mapper(staticTyping);
        String expected = "{\"value\":{\"a\":\"S:enclosing-7\"}}";
        assertEquals(expected, mapper.writeValueAsString(new DirectStringMap()));
        assertEquals(expected, mapper.writeValueAsString(new SameOutputType()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void enclosingConverterTakesPrecedenceWithDifferentOutputType(boolean staticTyping) {
        ObjectMapper mapper = mapper(staticTyping);
        String expected = "{\"value\":{\"a\":\"L:107\"}}";
        assertEquals(expected, mapper.writeValueAsString(new DirectLongMap()));
        assertEquals(expected, mapper.writeValueAsString(new DifferentOutputType()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void enclosingConverterWorksWithoutAccessorConverter(boolean staticTyping) {
        ObjectMapper mapper = mapper(staticTyping);
        String expected = "{\"value\":{\"a\":\"S:enclosing-7\"}}";
        assertEquals(expected, mapper.writeValueAsString(new DirectStringMap()));
        assertEquals(expected, mapper.writeValueAsString(new NoAccessorConverter()));
    }
}

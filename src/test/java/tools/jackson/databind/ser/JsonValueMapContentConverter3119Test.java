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

class JsonValueMapContentConverter3119Test
{
    public static class ToString extends StdConverter<Integer, String> {
        @Override
        public String convert(Integer value) {
            return value == 0 ? null : "converted-" + value;
        }
    }

    public static class StringContent extends ValueSerializer<String> {
        private final String prefix;

        public StringContent() {
            this("serialized-");
        }

        private StringContent(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
            // The override must retain its enclosing property context.
            if (!property.getType().hasRawClass(ConvertedMap.class)) {
                throw new IllegalArgumentException("Expected the enclosing wrapper property");
            }
            return new StringContent(property.getName() + "-");
        }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(prefix + value);
        }
    }

    public static class PrefixKey extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("key-" + value);
        }
    }

    public static class ConvertedMap {
        private final int value;

        public ConvertedMap(int value) {
            this.value = value;
        }

        @JsonValue
        @JsonSerialize(contentConverter = ToString.class)
        public Map<String, Integer> values() {
            return Map.of("a", value);
        }
    }

    public static class Holder {
        @JsonSerialize(contentUsing = StringContent.class)
        public ConvertedMap wrapped = new ConvertedMap(7);
    }

    public static class OtherHolder {
        @JsonSerialize(contentUsing = StringContent.class)
        public ConvertedMap other = new ConvertedMap(8);
    }

    public static class KeyOnlyHolder {
        @JsonSerialize(keyUsing = PrefixKey.class)
        public ConvertedMap wrapped = new ConvertedMap(7);
    }

    private ObjectMapper mapper(boolean staticTyping) {
        return staticTyping ? JsonMapper.builder().enable(MapperFeature.USE_STATIC_TYPING).build()
                : JsonMapper.builder().build();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void enclosingContentSerializerRunsAfterAccessorConverter(boolean staticTyping) {
        assertEquals("{\"wrapped\":{\"a\":\"wrapped-converted-7\"}}",
                mapper(staticTyping).writeValueAsString(new Holder()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void converterNullResultBypassesEnclosingContentSerializer(boolean staticTyping) {
        Holder holder = new Holder();
        holder.wrapped = new ConvertedMap(0);
        assertEquals("{\"wrapped\":{\"a\":null}}",
                mapper(staticTyping).writeValueAsString(holder));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void keyOnlyOverrideKeepsAccessorConverter(boolean staticTyping) {
        assertEquals("{\"wrapped\":{\"key-a\":\"converted-7\"}}",
                mapper(staticTyping).writeValueAsString(new KeyOnlyHolder()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void cachedContentOverridesKeepTheirEnclosingContext(boolean staticTyping) {
        ObjectMapper mapper = mapper(staticTyping);
        for (int i = 0; i < 3; ++i) {
            assertEquals("{\"wrapped\":{\"a\":\"wrapped-converted-7\"}}",
                    mapper.writeValueAsString(new Holder()));
            assertEquals("{\"other\":{\"a\":\"other-converted-8\"}}",
                    mapper.writeValueAsString(new OtherHolder()));
            assertEquals("{\"a\":\"converted-9\"}",
                    mapper.writeValueAsString(new ConvertedMap(9)));
        }
    }
}

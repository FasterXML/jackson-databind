package tools.jackson.databind.ser;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#3119]: `@JsonSerialize(keyUsing)` on a `@JsonValue` accessor returning
// a `Map` must be honored (fixed in 3.3.0 via [databind#4762])
class JsonValueKeySerializer3119Test extends DatabindTestUtil
{
    // Key type is an interface, as in the original report
    interface Id {
        String toCompactId();
    }

    record IdImpl(String id) implements Id {
        @Override
        public String toCompactId() {
            return "id-" + id;
        }
    }

    static class IdKeySerializer extends ValueSerializer<Id> {
        @Override
        public void serialize(Id value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName(value.toCompactId());
        }
    }

    static class PrefixKeySerializer extends ValueSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName("key-" + value);
        }
    }

    static class IdMapResponse {
        @JsonValue
        @JsonSerialize(keyUsing = IdKeySerializer.class)
        public Map<Id, Boolean> getMap() {
            return Map.of(new IdImpl("1"), true);
        }
    }

    static class GetterValue {
        @JsonValue
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        public Map<String, Integer> value() {
            return Map.of("a", 1);
        }
    }

    static class FieldValue {
        @JsonValue
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        public Map<String, Integer> value = Map.of("a", 1);
    }

    static class PlainValue {
        @JsonValue
        public Map<String, Integer> value() {
            return Map.of("a", 1);
        }
    }

    abstract static class ValueMixin {
        @JsonSerialize(keyUsing = PrefixKeySerializer.class)
        abstract Map<String, Integer> value();
    }

    static class Holder {
        public GetterValue wrapped = new GetterValue();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void interfaceKeyFromReport() {
        assertEquals("""
                {"id-1":true}""",
                MAPPER.writeValueAsString(new IdMapResponse()));
    }

    @Test
    void getterKeySerializer() {
        assertEquals("""
                {"key-a":1}""",
                MAPPER.writeValueAsString(new GetterValue()));
    }

    @Test
    void fieldKeySerializer() {
        assertEquals("""
                {"key-a":1}""",
                MAPPER.writeValueAsString(new FieldValue()));
    }

    @Test
    void mixinKeySerializer() {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(PlainValue.class, ValueMixin.class)
                .build();
        assertEquals("""
                {"key-a":1}""",
                mapper.writeValueAsString(new PlainValue()));
    }

    @Test
    void keySerializerAsPropertyValue() {
        assertEquals("""
                {"wrapped":{"key-a":1}}""",
                MAPPER.writeValueAsString(new Holder()));
    }
}

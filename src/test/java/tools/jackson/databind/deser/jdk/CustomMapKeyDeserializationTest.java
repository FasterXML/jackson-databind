package tools.jackson.databind.deser.jdk;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.a2q;
import static tools.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class CustomMapKeyDeserializationTest
{
    @JsonDeserialize(keyUsing = Key2454Deserializer.class)
    @JsonSerialize(keyUsing = Key2454Serializer.class)
    static class Key2454 {
        String id;

        public Key2454(String id, boolean bogus) {
            this.id = id;
        }
    }

    static class Key2454Deserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Key2454(key, false);
        }
    }

    static class Key2454Serializer extends ValueSerializer<Key2454> {
        @Override
        public void serialize(Key2454 value, JsonGenerator gen,
                SerializationContext serializers) {
            gen.writeName("id="+value.id);
        }
    }

    // [databind#4444]
    @JsonDeserialize(keyUsing = Key4444ForClass.class)
    static class Key4444 {
        final String value;

        Key4444(String value) {
            this.value = value;
        }
    }

    static class Key4444ForClass extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Key4444(key + "-class");
        }
    }

    static class Key4444ForMapper extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            return new Key4444(key + "-mapper");
        }
    }

    // [databind#6242]: delegating key deserializer (used for enum keys with a
    // custom `@JsonDeserialize(using = ...)` value deserializer) must report the
    // actual offending token, not the outer parser's unrelated current position.
    @JsonDeserialize(using = ColorKeyValueDeserializer.class)
    enum AnnotatedColorKey { RED, GREEN, BLUE }

    static class ColorKeyValueDeserializer extends StdDeserializer<AnnotatedColorKey> {
        ColorKeyValueDeserializer() { super(AnnotatedColorKey.class); }

        @Override
        public AnnotatedColorKey deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getString();
            try {
                return AnnotatedColorKey.valueOf(value);
            } catch (IllegalArgumentException e) {
                return (AnnotatedColorKey) ctxt.reportInputMismatch(AnnotatedColorKey.class,
                        "not a valid color: %s", value);
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // For [databind#2454]
    
    @Test
    public void testCustomSerializer() throws Exception
    {
        assertEquals(a2q("{'id=a':'b'}"),
                MAPPER.writeValueAsString(Collections.singletonMap(new Key2454("a", true), "b")));
    }

    @Test
    public void testCustomDeserializer() throws Exception
    {
        Map<Key2454, String> result = MAPPER.readValue(a2q("{'a':'b'}"),
                new TypeReference<Map<Key2454, String>>() { });
        assertEquals(1, result.size());
        Key2454 key = result.keySet().iterator().next();
        assertEquals("a", key.id);
    }

    // [databind#4444]: annotation-based key deserializer should not be overwritten by mapper-level one
    @Test
    public void testAnnotationKeyDeserWithoutMapperOverride4444() throws Exception {
        TypeReference<Map<Key4444, String>> typeRef = new TypeReference<Map<Key4444, String>>() {};
        Map<Key4444, String> result = MAPPER.readValue("{\"foo\":null}", typeRef);

        assertEquals("foo-class", result.keySet().stream().findFirst().get().value);
    }

    // [databind#4444]
    @Test
    public void testAnnotationKeyDeserWithMapperOverride4444() throws Exception {
        SimpleModule sm = new SimpleModule();
        sm.addKeyDeserializer(Key4444.class, new Key4444ForMapper());

        ObjectMapper mapper = jsonMapperBuilder().addModule(sm).build();
        TypeReference<Map<Key4444, String>> typeRef = new TypeReference<Map<Key4444, String>>() {};
        Map<Key4444, String> result = mapper.readValue("{\"foo\":null}", typeRef);

        assertEquals("foo-class", result.keySet().stream().findFirst().get().value);
    }

    // [databind#4680]: Custom key deserializer registered for `Object.class` on nested JSON
    @SuppressWarnings("unchecked")
    @Test
    public void testCustomKeyDeserializerNested4680() throws Exception
    {
        String json =
                """
                {
                     "name*": "Erik",
                     "address*": {
                         "city*": {
                             "id*": 1,
                             "name*": "Berlin"
                         },
                         "street*": "Elvirastr"
                     }
                 }
                """;

        SimpleModule keySanitizationModule = new SimpleModule("key-sanitization");
        keySanitizationModule.addKeyDeserializer(String.class, new KeyDeserializer() {
            @Override
            public String deserializeKey(String key, DeserializationContext ctxt) {
                return key.replace("*", "_");
            }
        });
        keySanitizationModule.addKeyDeserializer(Object.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) {
                return key.replace("*", "_");
            }
        });

        ObjectMapper mapper = JsonMapper.builder().addModule(keySanitizationModule).build();

        Map<String, Object> result = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        assertEquals("Erik", result.get("name_"));

        Map<String, Object> addressMap = (Map<String, Object>) result.get("address_");
        assertEquals("Elvirastr", addressMap.get("street_"));
        Map<String, Object> cityMap = (Map<String, Object>) addressMap.get("city_");
        assertEquals(1, cityMap.get("id_"));
        assertEquals("Berlin", cityMap.get("name_"));
    }

    // [databind#6242]
    @Test
    public void testDelegatingKeyDeserializerReportsOffendingToken() throws Exception
    {
        String json = a2q("{'RED':1,'notAColor':2}");
        TypeReference<Map<AnnotatedColorKey, Integer>> typeRef =
                new TypeReference<Map<AnnotatedColorKey, Integer>>() { };
        try (JsonParser input = MAPPER.createParser(json)) {
            MismatchedInputException e = assertThrows(MismatchedInputException.class,
                    () -> MAPPER.readValue(input, typeRef));
            JsonParser parser = assertInstanceOf(JsonParser.class, e.processor());
            assertEquals(JsonToken.VALUE_STRING, parser.currentToken());
            assertEquals("notAColor", parser.getString());
        }
    }
}

package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4680] Custom key deserializer registered for `Object.class` is ignored on nested JSON
public class CustomMapKeyDeserializer4680Test
{
    @SuppressWarnings("unchecked")
    @Test
    void testCustomKeyDeserializer()
            throws Exception
    {
        // GIVEN
        String json =
                "{\n" +
                        "     \"name*\": \"Erik\",\n" +
                        "     \"address*\": {\n" +
                        "         \"city*\": {\n" +
                        "             \"id*\": 1,\n" +
                        "             \"name*\": \"Berlin\"\n" +
                        "         },\n" +
                        "         \"street*\": \"Elvirastr\"\n" +
                        "     }\n" +
                        " }";

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

        // WHEN
        Map<String, Object> result = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        // THEN
        // depth 1 works as expected
        assertEquals("Erik", result.get("name_"));

        // before fix, depth 2 does NOT work as expected
        Map<String, Object> addressMap = (Map<String, Object>) result.get("address_");
        // before fix, null?? Fails here
        assertEquals("Elvirastr", addressMap.get("street_"));
        Map<String, Object> cityMap = (Map<String, Object>) addressMap.get("city_");
        assertEquals(1, cityMap.get("id_"));
        assertEquals("Berlin", cityMap.get("name_"));
    }

}
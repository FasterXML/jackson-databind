package com.fasterxml.jackson.databind.tofix;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

// [databind#4680] Custom key deserialiser registered for `Object.class` is ignored on nested JSON
public class CustomObjectKeyDeserializer4680Test
{

    @JacksonTestFailureExpected
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
        Assertions.assertEquals("Erik", result.get("name_"));

        // before fix, depth 2 does NOT work as expected
        Map<String, Object> addressMap = (Map<String, Object>) result.get("address_");
        // before fix, null?? Fails here
        Assertions.assertEquals("Elvirastr", addressMap.get("street_"));
        Map<String, Object> cityMap = (Map<String, Object>) addressMap.get("city_");
        Assertions.assertEquals(1, cityMap.get("id_"));
        Assertions.assertEquals("Berlin", cityMap.get("name_"));
    }

}
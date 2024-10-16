package com.fasterxml.jackson.databind.deser;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#4680] Custom key deserialiser registered for `Object.class` is ignored on nested JSON
public class CustomKeyDeserializer4680Test {

    // Reported test case
    @Test
    void customKeyDeserializerShouldBeUsedWhenTypeNotDefined() throws Exception {
        // GIVEN
        String JSON = "{\n" +
                "                    \"name*\": \"Erik\",\n" +
                "                    \"address*\": {\n" +
                "                        \"city*\": {\n" +
                "                            \"id*\": 1,\n" +
                "                            \"name*\": \"Berlin\"\n" +
                "                        },\n" +
                "                        \"street*\": \"Elvirastr\"\n" +
                "                    }\n" +
                "                }";

        ObjectMapper mapper = JsonMapper.builder().addModule(_customKeyDeserModule()).build();

        // WHEN
        Map<String, Object> result = mapper.readValue(JSON, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> addressMap = (Map<String, Object>) result.get("address_");
        Map<String, Object> cityMap = (Map<String, Object>) addressMap.get("city_");

        // THEN
        assertEquals("Erik", result.get("name_"));
        assertEquals("Elvirastr", addressMap.get("street_"));
        assertEquals(1, cityMap.get("id_"));
        assertEquals("Berlin", cityMap.get("name_"));
    }

    @Test
    void customKeyDeserFirst5KeysAsWell() throws Exception {
        // Given
        // test should check that first 5 keys per level get custom deserialized as well
        String JSON = "{\n" +
                "                    \"name1*\": \"Erik1\"," +
                "                    \"name2*\": \"Erik2\"," +
                "                    \"name3*\": \"Erik3\"," +
                "                    \"name4*\": \"Erik4\"," +
                "                    \"name5*\": \"Erik5\"," +
                "                    \"inner*\": {" +
                "                          \"key1*\": \"value1\"," +
                "                          \"key2*\": \"value2\"," +
                "                          \"key3*\": \"value3\"," +
                "                          \"key4*\": \"value4\"," +
                "                          \"key5*\": \"value5\"" +
                "                    }" +
                "                }";

        ObjectMapper mapper = JsonMapper.builder().addModule(_customKeyDeserModule()).build();

        // When
        Map<String, Object> outerMap = mapper.readValue(JSON, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> innerMap = (Map<String, Object>) outerMap.get("inner_");
        // Then
        // depth 1 works as expected
        assertEquals(6, outerMap.keySet().size());
        assertEquals(5, innerMap.keySet().size());

        // depth 2 works as expected
        assertTrue(outerMap.keySet().stream().allMatch(key -> key.endsWith("_")));
        assertTrue(innerMap.keySet().stream().allMatch(key -> key.endsWith("_")));
    }

    private Module _customKeyDeserModule() {
        return new SimpleModule("key-sanitization")
                .addKeyDeserializer(String.class, new KeyDeserializer() {
                    @Override
                    public String deserializeKey(String key, DeserializationContext ctxt) {
                        return key.replace("*", "_");
                    }
                })
                .addKeyDeserializer(Object.class, new KeyDeserializer() {
                    @Override
                    public Object deserializeKey(String key, DeserializationContext ctxt) {
                        return key.replace("*", "_");
                    }
                });
    }

}

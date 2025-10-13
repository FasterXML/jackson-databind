package com.fasterxml.jackson.databind.ser;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#5342] JsonAnyGetter method serialization can override JsonProperty serialization on serialized name conflict
public class AnyGetterNameConflictSerialization5342Test
    extends DatabindTestUtil
{
    public static class Pojo5342 {
        @JsonIgnore
        private Map<String, Object> additionalProperties;
        @JsonProperty(value = "additionalProperties")
        private Map<String, Object> additionalPropertiesProperty;

        @JsonAnySetter
        private void additionalProperties(String key, Object value) {
            if (additionalProperties == null) {
                additionalProperties = new HashMap<>();
            }
            additionalProperties.put(key.replace("\\.", "."), value);
        }

        @JsonAnyGetter
        public Map<String, Object> additionalProperties() {
            return additionalProperties;
        }

        public Map<String, Object> additionalPropertiesProperty() {
            return additionalPropertiesProperty;
        }

        public void additionalPropertiesProperty(Map<String, Object> additionalPropertiesProperty) {
            this.additionalPropertiesProperty = additionalPropertiesProperty;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testOverwrite()
        throws Exception
    {
        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("fizz", "buzz");
        Pojo5342 pojo = new Pojo5342();
        pojo.additionalProperties("foo", "bar");
        pojo.additionalPropertiesProperty(additionalProperties);


        String JSON = MAPPER.writeValueAsString(pojo);
        // was in 2.18 : {"foo":"bar","additionalProperties": {"fizz":"buzz"}}
        // now in 2.19 : {"foo":"bar"}... need FIX!
        assertTrue(JSON.contains("\"additionalProperties\":{\"fizz\":\"buzz\"}"));
        assertTrue(JSON.contains("\"foo\":\"bar\""));

        // Try deserializaing back
        Pojo5342 actual = MAPPER.readValue(JSON, Pojo5342.class);
        assertEquals(1, actual.additionalProperties.size());
        assertEquals(1, actual.additionalPropertiesProperty().size());
    }

}

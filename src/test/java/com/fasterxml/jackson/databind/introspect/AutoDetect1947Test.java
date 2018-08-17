package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

// Test(s) for [databind#1947], regression for 2.9
public class AutoDetect1947Test extends BaseMapTest
{
    static class Entity1947 {
        public int shouldBeDetected;
        public String shouldNotBeDetected;

        @JsonProperty
        public int getShouldBeDetected() {
            return shouldBeDetected;
        }

        public void setShouldBeDetected(int shouldBeDetected) {
            this.shouldBeDetected = shouldBeDetected;
        }

        public String getShouldNotBeDetected() {
            return shouldNotBeDetected;
        }

        public void setShouldNotBeDetected(String shouldNotBeDetected) {
            this.shouldNotBeDetected = shouldNotBeDetected;
        }
    }
    public void testDisablingAll() throws Exception
    {
        ObjectMapper mapper = objectMapperBuilder()
                .disable(MapperFeature.AUTO_DETECT_SETTERS)
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .build();
        String json = mapper.writeValueAsString(new Entity1947());
        JsonNode n = mapper.readTree(json);
        assertEquals(1, n.size());
        assertTrue(n.has("shouldBeDetected"));
        assertFalse(n.has("shouldNotBeDetected"));
    }
}

package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.UUID;

public class TestNpe597 extends BaseMapTest
{
    static class JsonEntity {
        protected final String type;
        protected final UUID id;

        private JsonEntity(String type, UUID id) {
            this.type = type;
            this.id = id;
        }

        @JsonCreator
        public static JsonEntity create(@JsonProperty("type") String type, @JsonProperty("id") UUID id) {
            if (type != null && !type.contains(" ") && (id != null)) {
                return new JsonEntity(type, id);
            }

            return null;
        }
    }

    public void testDeserialize() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{ \"type\" : \"     \", \"id\" : \"000c0ffb-a0d6-4d2e-a379-4aeaaf283599\" }";
        try {
            objectMapper.readValue(json, JsonEntity.class);
            fail("Should not have succeeded");
        } catch (JsonMappingException e) {
            e.printStackTrace();
            verifyException(e, "JSON creator returned null");
        }
    }
}

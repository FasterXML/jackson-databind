package com.fasterxml.jackson.databind.deser.jdk;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;

public class MapRawWithGeneric2846Test extends BaseMapTest
{
    @SuppressWarnings("rawtypes")
    static class GenericEntity<T> {
        public Map map;
    }

    static class SimpleEntity {
        public Integer number;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testIssue2821Part2() throws Exception {
        final String JSON = "{ \"map\": { \"key\": \"value\" } }";
        GenericEntity<SimpleEntity> genericEntity = MAPPER.readValue(JSON,
                new TypeReference<GenericEntity<SimpleEntity>>() {});
        assertNotNull(genericEntity);
    }
}

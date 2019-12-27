package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class TestCreatorNullPrimitives extends BaseMapTest {

    // [databind#2101]
    static class JsonEntity {
        protected final int x;
        protected final int y;

        @JsonCreator
        private JsonEntity(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class NestedJsonEntity {
        protected final JsonEntity entity;

        @JsonCreator
        private NestedJsonEntity(@JsonProperty("entity") JsonEntity entity) {
            this.entity = entity;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // [databind#2101]: ensure that the property is included in the path
    public void testCreatorNullPrimitive() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        String json = aposToQuotes("{'x': 2}");
        try {
            objectMapper.readValue(json, JsonEntity.class);
            fail("Should not have succeeded");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot map `null` into type int");
            assertEquals(1, e.getPath().size());
            assertEquals("y", e.getPath().get(0).getFieldName());
        }
    }

    public void testCreatorNullPrimitiveInNestedObject() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        String json = aposToQuotes("{ 'entity': {'x': 2}}");
        try {
            objectMapper.readValue(json, NestedJsonEntity.class);
            fail("Should not have succeeded");
        } catch (JsonMappingException e) {
            verifyException(e, "Cannot map `null` into type int");
            assertEquals(2, e.getPath().size());
            assertEquals("y", e.getPath().get(1).getFieldName());
            assertEquals("entity", e.getPath().get(0).getFieldName());
        }
    }
}

package com.fasterxml.jackson.databind.deser.creators;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

public class TestCreatorNullPrimitives
{
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

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2101]: ensure that the property is included in the path
    @Test
    public void testCreatorNullPrimitive() throws IOException {
        final ObjectReader r = MAPPER.readerFor(JsonEntity.class)
            .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        String json = a2q("{'x': 2}");
        try {
            r.readValue(json);
            fail("Should not have succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot map `null` into type `int`");
            assertEquals(1, e.getPath().size());
            assertEquals("y", e.getPath().get(0).getFieldName());
        }
    }

    @Test
    public void testCreatorNullPrimitiveInNestedObject() throws IOException {
        final ObjectReader r = MAPPER.readerFor(NestedJsonEntity.class)
                .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        String json = a2q("{ 'entity': {'x': 2}}");
        try {
            r.readValue(json);
            fail("Should not have succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot map `null` into type `int`");
            assertEquals(2, e.getPath().size());
            assertEquals("y", e.getPath().get(1).getFieldName());
            assertEquals("entity", e.getPath().get(0).getFieldName());
        }
    }
}

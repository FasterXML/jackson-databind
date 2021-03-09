package com.fasterxml.jackson.databind.seq;

import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

public class ReadTreesTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class IdValue {
        public int id, value;
    }

    /*
    /**********************************************************
    /* Unit tests; happy case
    /**********************************************************
     */

    public void testReadTreeSequence() throws Exception
    {
        final String INPUT = a2q(
                "{\"id\":1, \"value\":137 }\n" +
                "{\"id\":2, \"value\":256 }\n" +
                "{\"id\":3, \"value\":-89 }");
        try (MappingIterator<JsonNode> it = MAPPER.readerFor(JsonNode.class)
                .readValues(INPUT)) {
            assertTrue(it.hasNextValue());
            JsonNode node = it.nextValue();
            assertEquals("{\"id\":1,\"value\":137}", node.toString());
            assertEquals(1, node.path("id").intValue());
            assertEquals(1, node.path("id").asInt());

            assertTrue(it.hasNextValue());
            node = it.nextValue();
            assertEquals("{\"id\":2,\"value\":256}", node.toString());

            assertTrue(it.hasNextValue());
            node = it.nextValue();
            assertEquals("{\"id\":3,\"value\":-89}", node.toString());

            assertFalse(it.hasNextValue());
        }

        // Or with "readAll()":
        try (MappingIterator<JsonNode> it = MAPPER.readerFor(JsonNode.class)
                .readValues(INPUT)) {
            List<JsonNode> all = it.readAll();
            assertEquals(3, all.size());
            assertEquals("{\"id\":3,\"value\":-89}", all.get(2).toString());
        }
    }

    /*
    /**********************************************************
    /* Unit tests; error recovery
    /**********************************************************
     */

    public void testReadPOJOHandleFail() throws Exception
    {
        final String INPUT = a2q(
                "{\"id\":1, \"value\":137 }\n" +
                "{\"id\":2, \"value\":\"foobar\" }\n" +
                "{\"id\":3, \"value\":-89 }");

        try (MappingIterator<IdValue> it = MAPPER.readerFor(IdValue.class)
                .readValues(INPUT)) {
            assertTrue(it.hasNextValue());
            IdValue v = it.nextValue();
            assertEquals(137, v.value);

            assertTrue(it.hasNextValue());
            // fun part:
            try {
                v = it.nextValue();
                fail("Should catch the problem");
            } catch (InvalidFormatException e) {
                verifyException(e, "Cannot deserialize value");
            }

            assertTrue(it.hasNextValue());
            v = it.nextValue();
            assertEquals(-89, v.value);

            assertFalse(it.hasNextValue());
        }
    }
}

package com.fasterxml.jackson.databind.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.stream.Stream;

public class StreamCollectorTest extends BaseMapTest {

    public void testEmpty() {
        final ArrayNode jsonArray = Stream.<JsonNode>empty()
                .collect(JacksonCollector.toJsonList());
        assertNotNull(jsonArray);
        assertEquals(0, jsonArray.size());
        assertEquals(
                "[]",
                jsonArray.toString()
        );
    }

    public void testOneElementStream() throws JsonProcessingException {
        final ObjectMapper objectMapper = objectMapper();
        final ArrayNode jsonArray = Stream.<JsonNode>of(
                objectMapper.valueToTree(new Point(1, 2))
        ).collect(JacksonCollector.toJsonList());

        assertNotNull(jsonArray);
        assertEquals(1, jsonArray.size());
        assertEquals(
                "[{\"x\":1,\"y\":2}]",
                jsonArray.toString()
        );
    }

    public void testManyElementsStream() {
        final ObjectMapper objectMapper = objectMapper();
        final ArrayNode jsonArray = Stream.<JsonNode>of(
                objectMapper.valueToTree(new Point(1, 2)),
                objectMapper.valueToTree(new Point(3, 4)),
                objectMapper.valueToTree(new Point(5, 6))
        ).collect(JacksonCollector.toJsonList());

        assertNotNull(jsonArray);
        assertEquals(3, jsonArray.size());
        assertEquals(
                "[{\"x\":1,\"y\":2},{\"x\":3,\"y\":4},{\"x\":5,\"y\":6}]",
                jsonArray.toString()
        );
    }
}

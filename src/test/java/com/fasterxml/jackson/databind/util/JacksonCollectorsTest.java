package com.fasterxml.jackson.databind.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class JacksonCollectorsTest {

    @Test
    public void testToArrayNode()
    {
        final ObjectMapper objectMapper = new ObjectMapper();

        final JsonNode jsonNodeResult = IntStream.range(0, 10)
            .mapToObj(i -> {
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("testString", "example");
                objectNode.put("testNumber", i);
                objectNode.put("testBoolean", true);

                return objectNode;
            })
            .collect(JacksonCollectors.toArrayNode());

        assertEquals(10, jsonNodeResult.size());
        jsonNodeResult.forEach(jsonNode -> assertFalse(jsonNode.isEmpty()));
    }
}

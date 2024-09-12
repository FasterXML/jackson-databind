package tools.jackson.databind.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class JacksonCollectorsTest {

    @Test
    public void testToJsonNode()
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
            .collect(JacksonCollectors.toJsonNode());

        System.out.println(jsonNodeResult.toPrettyString());

        assertEquals(10, jsonNodeResult.size());
        jsonNodeResult.forEach(jsonNode -> assertFalse(jsonNode.isEmpty()));
    }
}

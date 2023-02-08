package com.fasterxml.jackson.failing;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.*;

// [databind#3262]: The issue is that
// `JsonNode.toString()` will use internal "default" ObjectMapper which
// does not (and cannot) have modules for external datatypes, such as
// Java 8 Date/Time types. So we'll catch IOException/RuntimeException for
// POJONode, produce something like "[ERROR: (type) [msg]" TextNode for that case?
public class POJONode3262Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAddJava8DateAsPojo() throws Exception
    {
        JsonNode node = MAPPER.createObjectNode().putPOJO("test", LocalDateTime.now());
        String json = node.toString();
        assertNotNull(json);

        JsonNode result = MAPPER.readTree(json);
        String msg = result.path("test").asText();
        assertTrue("Wrong fail message: "+msg,
                msg.startsWith("[ERROR:"));
        assertTrue("Wrong fail message: "+msg,
                msg.contains("InvalidDefinitionException"));
    }
}

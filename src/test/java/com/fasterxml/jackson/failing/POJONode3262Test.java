package com.fasterxml.jackson.failing;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.*;

// [databind#3262]: not sure what could be done here. The issue is that
// `JsonNode.toString()` will use internal "default" ObjectMapper which
// does not (and cannot) have modules for external datatypes, such as
// Java 8 Date/Time types. One possibility would be catch IOException for
// POJONode, produce something like "ERROR: <ExceptionClass>" TextNode for that case?
public class POJONode3262Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testAddJava8DateAsPojo() throws Exception
    {
        JsonNode node = MAPPER.createObjectNode().putPOJO("test", LocalDateTime.now());
        String json = node.toString();

        assertNotNull(json);
    }
}

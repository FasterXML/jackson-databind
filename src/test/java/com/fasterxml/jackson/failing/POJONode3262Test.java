package com.fasterxml.jackson.failing;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.*;

// [databind#3262]: not sure what could be done here
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

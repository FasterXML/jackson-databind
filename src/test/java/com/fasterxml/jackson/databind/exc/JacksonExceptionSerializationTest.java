package com.fasterxml.jackson.databind.exc;

import java.util.Map;

import com.fasterxml.jackson.core.JacksonException;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonExceptionSerializationTest extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#3244]: StackOverflow for basic JsonProcessingException?
    public void testIssue3244() throws Exception {
        JacksonException e = null;
        try {
            MAPPER.readValue("{ foo ", Map.class);
            fail("Should not pass");
        } catch (JacksonException e0) {
            e = e0;
        }
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(e);
//System.err.println("JSON: "+json);

        // Could try proper validation, but for now just ensure we won't crash
        assertNotNull(json);
        JsonNode n = MAPPER.readTree(json);
        assertTrue(n.isObject());
    }
}

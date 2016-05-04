package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying that simple exceptions can be serialized.
 */
public class TestExceptionSerialization
    extends BaseMapTest
{
    @SuppressWarnings("serial")
    @JsonIgnoreProperties({ "bogus1" })
    static class ExceptionWithIgnoral extends RuntimeException
    {
        public int bogus1 = 3;

        public int bogus2 = 5;

        protected ExceptionWithIgnoral() { }
        public ExceptionWithIgnoral(String msg) {
            super(msg);
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testSimple() throws Exception
    {
        String TEST = "test exception";
        Map<String,Object> result = writeAndMap(MAPPER, new Exception(TEST));
        // JDK 7 has introduced a new property 'suppressed' to Throwable
        Object ob = result.get("suppressed");
        if (ob != null) {
            assertEquals(5, result.size());
        } else {
            assertEquals(4, result.size());
        }

        assertEquals(TEST, result.get("message"));
        assertNull(result.get("cause"));
        assertEquals(TEST, result.get("localizedMessage"));

        // hmmh. what should we get for stack traces?
        Object traces = result.get("stackTrace");
        if (!(traces instanceof List<?>)) {
            fail("Expected a List for exception member 'stackTrace', got: "+traces);
        }
    }

    // for [databind#877]
    @SuppressWarnings("unchecked")
    public void testIgnorals() throws Exception
    {
        ExceptionWithIgnoral input = new ExceptionWithIgnoral("foobar");
        input.initCause(new IOException("surprise!"));

        // First, should ignore anything with class annotations
        String json = MAPPER
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(input);

        Map<String,Object> result = MAPPER.readValue(json, Map.class);
        assertEquals("foobar", result.get("message"));

        assertNull(result.get("bogus1"));
        assertNotNull(result.get("bogus2"));

        // and then also remova second property with config overrides
        ObjectMapper mapper = new ObjectMapper();
        mapper.configOverride(ExceptionWithIgnoral.class)
            .setIgnorals(JsonIgnoreProperties.Value.forIgnoredProperties("bogus2"));
        String json2 = mapper
                .writeValueAsString(new ExceptionWithIgnoral("foobar"));

        Map<String,Object> result2 = mapper.readValue(json2, Map.class);
        assertNull(result2.get("bogus1"));
        assertNull(result2.get("bogus2"));

        // and try to deserialize as well
        ExceptionWithIgnoral output = mapper.readValue(json2, ExceptionWithIgnoral.class);
        assertNotNull(output);
        assertEquals("foobar", output.getMessage());
    }
}

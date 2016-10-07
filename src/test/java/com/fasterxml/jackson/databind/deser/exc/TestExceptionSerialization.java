package com.fasterxml.jackson.databind.deser.exc;

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

    // [databind#1368]
    static class NoSerdeConstructor {
        private String strVal;
        public String getVal() { return strVal; }
        public NoSerdeConstructor( String strVal ) {
            this.strVal = strVal;
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

    // [databind#1368]
    public void testJsonMappingExceptionSerialization() throws IOException {
        Exception e = null;
        // cant deserialize due to unexpected constructor
        try {
            MAPPER.readValue( "{ \"val\": \"foo\" }", NoSerdeConstructor.class );
            fail("Should not pass");
        } catch (JsonMappingException e0) {
            verifyException(e0, "no suitable constructor");
            e = e0;
        }
        // but should be able to serialize new exception we got
        String json = MAPPER.writeValueAsString(e);
        JsonNode root = MAPPER.readTree(json);
        String msg = root.path("message").asText();
        String MATCH = "no suitable constructor";
        if (!msg.contains(MATCH)) {
            fail("Exception should contain '"+MATCH+"', does not: '"+msg+"'");
        }
    }
}

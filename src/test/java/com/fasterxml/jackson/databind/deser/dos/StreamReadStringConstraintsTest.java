package com.fasterxml.jackson.databind.deser.dos;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Tests for {@a href="https://github.com/FasterXML/jackson-core/issues/863"}.
 */
public class StreamReadStringConstraintsTest extends BaseMapTest
{
    final static class StringWrapper
    {
        String string;

        StringWrapper() { }

        StringWrapper(String string) { this.string = string; }

        void setString(String string) {
            this.string = string;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectMapper newJsonMapperWithUnlimitedStringSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    public void testBigString() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("string", 5001000), StringWrapper.class);
            fail("expected JsonMappingException");
        } catch (DatabindException e) {
            assertTrue("unexpected exception message: " + e.getMessage(),
                    e.getMessage().startsWith("String length (5001000) exceeds the maximum length (5000000)"));
        }
    }

    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("string", 6_000_000), StringWrapper.class);
            fail("expected JsonMappingException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 6000000 value
            assertTrue("unexpected exception message: " + message, message.startsWith("String length"));
            assertTrue("unexpected exception message: " + message, message.contains("exceeds the maximum length (5000000)"));
        }
    }

    public void testUnlimitedString() throws Exception
    {
        final int len = 5_001_000;
        StringWrapper sw = newJsonMapperWithUnlimitedStringSizeSupport()
                .readValue(generateJson("string", len), StringWrapper.class);
        assertEquals(len, sw.string.length());
    }


    private String generateJson(final String fieldName, final int len) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"")
                .append(fieldName)
                .append("\": \"");
        for (int i = 0; i < len; i++) {
            sb.append('a');
        }
        sb.append("\"}");
        return sb.toString();
    }
}

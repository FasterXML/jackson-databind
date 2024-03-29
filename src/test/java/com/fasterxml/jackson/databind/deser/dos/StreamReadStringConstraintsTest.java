package com.fasterxml.jackson.databind.deser.dos;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Tests for <a href="https://github.com/FasterXML/jackson-core/issues/863">databind#863</a>"
 */
public class StreamReadStringConstraintsTest
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

    private final static int TOO_LONG_STRING_VALUE = StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 100;
    
    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectMapper newJsonMapperWithUnlimitedStringSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    @Test
    public void testBigString() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("string", TOO_LONG_STRING_VALUE), StringWrapper.class);
            fail("expected DatabindException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            assertTrue(message.startsWith("String value length"), "unexpected exception message: " + message);
            assertTrue(message.contains("exceeds the maximum allowed ("), "unexpected exception message: " + message);
        }
    }

    @Test
    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("string", TOO_LONG_STRING_VALUE), StringWrapper.class);
            fail("expected JsonMappingException");
        } catch (DatabindException e) {
            final String message = e.getMessage();
            // this test fails when the TextBuffer is being resized, so we don't yet know just how big the string is
            // so best not to assert that the String length value in the message is the full 6000000 value
            assertTrue(message.startsWith("String value length"), "unexpected exception message: " + message);
            assertTrue(message.contains("exceeds the maximum allowed ("), "unexpected exception message: " + message);
        }
    }

    @Test
    public void testUnlimitedString() throws Exception
    {
        final int len = TOO_LONG_STRING_VALUE;
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

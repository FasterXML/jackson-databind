package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class TestBigStrings extends BaseMapTest
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
    /**********************************************************
    /* Tests
    /**********************************************************
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
            MAPPER.readValue(generateJson("string", 1001000), StringWrapper.class);
            fail("expected JsonMappingException");
        } catch (JsonMappingException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("String length (1001000) exceeds the maximum length (1000000)"));
        }
    }

    public void testBiggerString() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("string", 2000000), StringWrapper.class);
            fail("expected JsonMappingException");
        } catch (JsonMappingException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("String length (1048576) exceeds the maximum length (1000000)"));
        }
    }

    public void testUnlimitedString() throws Exception
    {
        final int len = 1001000;
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

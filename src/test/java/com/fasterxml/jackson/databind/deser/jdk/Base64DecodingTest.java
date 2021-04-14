package com.fasterxml.jackson.databind.deser.jdk;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

// Mostly for [databind#1425]; not in optimal place (as it also has
// tree-access tests), but has to do for now
public class Base64DecodingTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    private final byte[] HELLO_BYTES = "hello".getBytes(StandardCharsets.UTF_8);
    private final String BASE64_HELLO = "aGVsbG8=";

    // for [databind#1425]
    public void testInvalidBase64() throws Exception
    {
        byte[] b = MAPPER.readValue(q(BASE64_HELLO), byte[].class);
        assertEquals(HELLO_BYTES, b);

        _testInvalidBase64(MAPPER, BASE64_HELLO+"!");
        _testInvalidBase64(MAPPER, BASE64_HELLO+"!!");
    }

    private void _testInvalidBase64(ObjectMapper mapper, String value) throws Exception
    {
        // First, use data-binding
        try {
            MAPPER.readValue(q(value), byte[].class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Failed to decode");
            verifyException(e, "as base64");
            verifyException(e, "Illegal character '!'");
        }

        // and then tree model
        JsonNode tree = mapper.readTree(String.format("{\"foo\":\"%s\"}", value));
        JsonNode nodeValue = tree.get("foo");
        try {
            /*byte[] b =*/ nodeValue.binaryValue();
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot access contents of TextNode as binary");
            verifyException(e, "Illegal character '!'");
        }
    }
}

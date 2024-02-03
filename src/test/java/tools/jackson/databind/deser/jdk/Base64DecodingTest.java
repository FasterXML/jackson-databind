package tools.jackson.databind.deser.jdk;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

// Mostly for [databind#1425]; not in optimal place (as it also has
// tree-access tests), but has to do for now
public class Base64DecodingTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final byte[] HELLO_BYTES = "hello".getBytes(StandardCharsets.UTF_8);
    private final String BASE64_HELLO = "aGVsbG8=";

    // for [databind#1425]
    @Test
    public void testInvalidBase64() throws Exception
    {
        byte[] b = MAPPER.readValue(q(BASE64_HELLO), byte[].class);
        assertArrayEquals(HELLO_BYTES, b);

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
            // 16-Jan-2021, tatu: Message changed for 3.0 a bit
//            verifyException(e, "Failed to decode");
            verifyException(e, "Cannot deserialize value of type `byte[]` from String");
            verifyException(e, "Illegal character '!'");
            verifyException(e, "in base64 content");
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

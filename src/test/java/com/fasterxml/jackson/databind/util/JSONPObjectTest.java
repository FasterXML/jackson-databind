package com.fasterxml.jackson.databind.util;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class JSONPObjectTest extends DatabindTestUtil
{
    private final String CALLBACK = "callback";
    private final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Unit tests for checking that JSONP breaking characters U+2028 and U+2029 are escaped when creating a {@link JSONPObject}.
     */
    @Test
    public void testU2028Escaped() throws IOException {
        String containsU2028 = String.format("This string contains %c char", '\u2028');
        JSONPObject jsonpObject = new JSONPObject(CALLBACK, containsU2028);
        String valueAsString = MAPPER.writeValueAsString(jsonpObject);
        assertFalse(valueAsString.contains("\u2028"));
    }

    @Test
    public void testU2029Escaped() throws IOException {
        String containsU2029 = String.format("This string contains %c char", '\u2029');
        JSONPObject jsonpObject = new JSONPObject(CALLBACK, containsU2029);
        String valueAsString = MAPPER.writeValueAsString(jsonpObject);
        assertFalse(valueAsString.contains("\u2029"));
    }

    @Test
    public void testU2030NotEscaped() throws IOException {
        String containsU2030 = String.format("This string contains %c char", '\u2030');
        JSONPObject jsonpObject = new JSONPObject(CALLBACK, containsU2030);
        String valueAsString = MAPPER.writeValueAsString(jsonpObject);
        assertTrue(valueAsString.contains("\u2030"));
    }
}

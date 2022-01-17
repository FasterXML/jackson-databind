package com.fasterxml.jackson.databind.ser.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonIgnoreIf;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.deser.JsonIgnoreValidator;

import java.util.Map;

/**
 * This unit test suite tests use of {@link JsonIgnoreIf} annotations
 * with  bean serialization.
 */
public class TestSimpleSerializationIgnoreIf
        extends BaseMapTest
{

    // Class representing simple JsonIgnoreValidator
    public static class JsonIgnoreTrue extends JsonIgnoreValidator {
        @Override
        public boolean ignore() {
            return true;
        }
    }

    // Class representing simple JsonIgnoreValidator
    public static class JsonIgnoreFalse extends JsonIgnoreValidator {
        @Override
        public boolean ignore() {
            return false;
        }
    }

    // Class for testing enabled {@link JsonIgnoreIf} annotation
    final static class SizeClassEnabledIgnore
    {
        // should be seen
        @JsonIgnoreIf(JsonIgnoreFalse.class)
        public int x = 4;

        // should not be seen
        @JsonIgnoreIf(JsonIgnoreTrue.class)
        public int y = 2;
    }

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleIgnoreIf() throws Exception
    {
        // Should see "x", not "y"
        Map<String,Object> result = writeAndMap(MAPPER, new TestSimpleSerializationIgnoreIf.SizeClassEnabledIgnore());
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(4), result.get("x"));
        assertNull(result.get("y"));
    }

}
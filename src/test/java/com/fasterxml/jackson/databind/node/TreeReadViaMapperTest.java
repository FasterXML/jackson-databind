package com.fasterxml.jackson.databind.node;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.TestTreeDeserialization.Bean;

/**
 * This unit test suite tries to verify that ObjectMapper
 * can properly parse JSON and bind contents into appropriate
 * JsonNode instances.
 */
public class TreeReadViaMapperTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    public void testSimple() throws Exception
    {
        final String JSON = SAMPLE_DOC_JSON_SPEC;

        for (int type = 0; type < 2; ++type) {
            JsonNode result;

            if (type == 0) {
                result = MAPPER.readTree(new StringReader(JSON));
            } else {
                result = MAPPER.readTree(JSON);
            }

            assertType(result, ObjectNode.class);
            assertEquals(1, result.size());
            assertTrue(result.isObject());

            ObjectNode main = (ObjectNode) result;
            assertEquals("Image", main.fieldNames().next());
            JsonNode ob = main.elements().next();
            assertType(ob, ObjectNode.class);
            ObjectNode imageMap = (ObjectNode) ob;

            assertEquals(5, imageMap.size());
            ob = imageMap.get("Width");
            assertTrue(ob.isIntegralNumber());
            assertFalse(ob.isFloatingPointNumber());
            assertEquals(SAMPLE_SPEC_VALUE_WIDTH, ob.intValue());
            ob = imageMap.get("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_HEIGHT, ob.intValue());

            ob = imageMap.get("Title");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TITLE, ob.textValue());

            ob = imageMap.get("Thumbnail");
            assertType(ob, ObjectNode.class);
            ObjectNode tn = (ObjectNode) ob;
            ob = tn.get("Url");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, ob.textValue());
            ob = tn.get("Height");
            assertTrue(ob.isIntegralNumber());
            assertEquals(SAMPLE_SPEC_VALUE_TN_HEIGHT, ob.intValue());
            ob = tn.get("Width");
            assertTrue(ob.isTextual());
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, ob.textValue());

            ob = imageMap.get("IDs");
            assertTrue(ob.isArray());
            ArrayNode idList = (ArrayNode) ob;
            assertEquals(4, idList.size());
            assertEquals(4, calcLength(idList.elements()));
            assertEquals(4, calcLength(idList.iterator()));
            {
                int[] values = new int[] {
                    SAMPLE_SPEC_VALUE_TN_ID1,
                    SAMPLE_SPEC_VALUE_TN_ID2,
                    SAMPLE_SPEC_VALUE_TN_ID3,
                    SAMPLE_SPEC_VALUE_TN_ID4
                };
                for (int i = 0; i < values.length; ++i) {
                    assertEquals(values[i], idList.get(i).intValue());
                }
                int i = 0;
                for (JsonNode n : idList) {
                    assertEquals(values[i], n.intValue());
                    ++i;
                }
            }
        }
    }

    public void testMixed() throws IOException
    {
        String JSON = "{\"node\" : { \"a\" : 3 }, \"x\" : 9 }";
        Bean bean = MAPPER.readValue(JSON, Bean.class);

        assertEquals(9, bean._x);
        JsonNode n = bean._node;
        assertNotNull(n);
        assertEquals(1, n.size());
        ObjectNode on = (ObjectNode) n;
        assertEquals(3, on.get("a").intValue());
    }

    /**
     * Type mappers should be able to gracefully deal with end of
     * input.
     */
    public void testEOF() throws Exception
    {
        String JSON =
            "{ \"key\": [ { \"a\" : { \"name\": \"foo\",  \"type\": 1\n"
            +"},  \"type\": 3, \"url\": \"http://www.google.com\" } ],\n"
            +"\"name\": \"xyz\", \"type\": 1, \"url\" : null }\n  "
            ;
        JsonFactory jf = new JsonFactory();
        JsonParser p = jf.createParser(new StringReader(JSON));
        JsonNode result = MAPPER.readTree(p);

        assertTrue(result.isObject());
        assertEquals(4, result.size());

        assertNull(MAPPER.readTree(p));
        p.close();
    }

    public void testNullViaParser() throws Exception
    {
        final String JSON = " null ";
        JsonFactory jf = new JsonFactory();

        try (JsonParser p = jf.createParser(new StringReader(JSON))) {
            final JsonNode result = MAPPER.readTree(p);
            assertTrue(result.isNull());
        }
    }

    public void testMultiple() throws Exception
    {
        String JSON = "12  \"string\" [ 1, 2, 3 ]";
        JsonFactory jf = new JsonFactory();
        JsonParser p = jf.createParser(new StringReader(JSON));
        final ObjectMapper mapper = objectMapper();
        JsonNode result = mapper.readTree(p);

        assertTrue(result.isIntegralNumber());
        assertTrue(result.isInt());
        assertFalse(result.isTextual());
        assertEquals(12, result.intValue());

        result = mapper.readTree(p);
        assertTrue(result.isTextual());
        assertFalse(result.isIntegralNumber());
        assertFalse(result.isInt());
        assertEquals("string", result.textValue());

        result = mapper.readTree(p);
        assertTrue(result.isArray());
        assertEquals(3, result.size());

        assertNull(mapper.readTree(p));
        p.close();
    }

    /*
    /**********************************************
    /* Helper methods
    /**********************************************
     */

    private int calcLength(Iterator<JsonNode> it)
    {
        int count = 0;
        while (it.hasNext()) {
            it.next();
            ++count;
        }
        return count;
    }
}


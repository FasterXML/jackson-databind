package com.fasterxml.jackson.databind.node;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * This unit test suite tries to verify that the trees ObjectMapper
 * constructs can be serialized properly.
 */
public class TestTreeMapperSerializer extends NodeTestBase
{
    final static String FIELD1 = "first";
    final static String FIELD2 = "Second?";
    final static String FIELD3 = "foo'n \"bar\"";
    final static String FIELD4 = "4";

    final static String TEXT1 = "Some text & \"stuff\"";
    final static String TEXT2 = "Some more text:\twith\nlinefeeds and all!";

    final static double DOUBLE_VALUE = 9.25;

    private final ObjectMapper mapper = newObjectMapper();

    public void testFromArray() throws Exception
    {
        ArrayNode root = mapper.createArrayNode();
        root.add(TEXT1);
        root.add(3);
        ObjectNode obj = root.addObject();
        obj.put(FIELD1, true);
        obj.putArray(FIELD2);
        root.add(false);

        /* Ok, ready... let's serialize using one of two alternate
         * methods: first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = mapper.createGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }
            verifyFromArray(sw.toString());
        }
            
        // And then convenient but less efficient alternative:
        verifyFromArray(mapper.writeValueAsString(root));
    }

    public void testFromMap() throws Exception
    {
        ObjectNode root = mapper.createObjectNode();
        root.put(FIELD4, TEXT2);
        root.put(FIELD3, -1);
        root.putArray(FIELD2);
        root.put(FIELD1, DOUBLE_VALUE);

        /* Let's serialize using one of two alternate methods:
         * first preferred (using generator)
         * (there are 2 variants here too)
         */
        for (int i = 0; i < 2; ++i) {
            StringWriter sw = new StringWriter();
            if (i == 0) {
                JsonGenerator gen = mapper.createGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }
            verifyFromMap(sw.toString());
        }

        // And then convenient but less efficient alternative:
        verifyFromMap(mapper.writeValueAsString(root));
    }

    /**
     * Unit test to check for regression of [JACKSON-18].
     */
    public void testSmallNumbers()
        throws Exception
    {
        ArrayNode root = mapper.createArrayNode();
        for (int i = -20; i <= 20; ++i) {
            JsonNode n = root.numberNode(i);
            root.add(n);
            // Hmmh. Not sure why toString() won't be triggered otherwise...
            assertEquals(String.valueOf(i), n.toString());
        }

        // Loop over 2 different serialization methods
        for (int type = 0; type < 2; ++type) {
            StringWriter sw = new StringWriter();
            if (type == 0) {
                JsonGenerator gen = mapper.createGenerator(sw);
                root.serialize(gen, null);
                gen.close();
            } else {
                mapper.writeValue(sw, root);
            }

            String doc = sw.toString();
            JsonParser p = mapper.createParser(new StringReader(doc));
            
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = -20; i <= 20; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
                assertEquals(""+i, p.getText());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    public void testBinary() throws Exception
    {
        final int LENGTH = 13045;
        byte[] data = new byte[LENGTH];
        for (int i = 0; i < LENGTH; ++i) {
            data[i] = (byte) i;
        }
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, BinaryNode.valueOf(data));

        JsonParser p = mapper.createParser(sw.toString());
        // note: can't determine it's binary from json alone:
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertArrayEquals(data, p.getBinaryValue());
        p.close();
    }

    /*
    ///////////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////////
     */

    private void verifyFromArray(String input)
        throws Exception
    {
        JsonParser p = mapper.createParser(new StringReader(input));
        
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(TEXT1, getAndVerifyText(p));

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD1, getAndVerifyText(p));
        
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD2, getAndVerifyText(p));
        
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    private void verifyFromMap(String input)
        throws Exception
    {
        JsonParser p = mapper.createParser(input);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD4, getAndVerifyText(p));
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(TEXT2, getAndVerifyText(p));
        
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD3, getAndVerifyText(p));
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-1, p.getIntValue());
        
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD2, getAndVerifyText(p));
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(FIELD1, getAndVerifyText(p));
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(DOUBLE_VALUE, p.getDoubleValue(), 0);
        
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        
        assertNull(p.nextToken());
        p.close();
    }
}

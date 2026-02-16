package tools.jackson.databind.ser;

import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.TokenBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings, arrays,
 * and Jackson-specific types.
 */
public class SimpleTypeSerializationTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Test methods, simple types
    /**********************************************************
     */

    @Test
    public void testBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(Boolean.TRUE));
        assertEquals("false", MAPPER.writeValueAsString(Boolean.FALSE));
    }

    @Test
    public void testBooleanArray() throws Exception
    {
        assertEquals("[true,false]", MAPPER.writeValueAsString(new boolean[] { true, false} ));
        assertEquals("[true,false]", MAPPER.writeValueAsString(new Boolean[] { Boolean.TRUE, Boolean.FALSE} ));
    }

    @Test
    public void testByteArray() throws Exception
    {
        byte[] data = { 1, 17, -3, 127, -128 };
        Byte[] data2 = new Byte[data.length];
        for (int i = 0; i < data.length; ++i) {
            data2[i] = data[i]; // auto-boxing
        }
        // For this we need to deserialize, to get base64 codec
        String str1 = MAPPER.writeValueAsString(data);
        String str2 = MAPPER.writeValueAsString(data2);
        assertArrayEquals(data, MAPPER.readValue(str1, byte[].class));
        assertArrayEquals(data2, MAPPER.readValue(str2, Byte[].class));
    }

    // as per [Issue#42], allow Base64 variant use as well
    @Test
    public void testBase64Variants() throws Exception
    {
        final byte[] INPUT = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890X".getBytes("UTF-8");

        // default encoding is "MIME, no linefeeds", so:
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="), MAPPER.writeValueAsString(INPUT));
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.MIME_NO_LINEFEEDS).writeValueAsString(INPUT));

        // but others should be slightly different
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1\\ndnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.MIME).writeValueAsString(INPUT));
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA"), // no padding or LF
                MAPPER.writer(Base64Variants.MODIFIED_FOR_URL).writeValueAsString(INPUT));
        // PEM mandates 64 char lines:
        assertEquals(q("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwYWJjZGVmZ2hpamts\\nbW5vcHFyc3R1dnd4eXoxMjM0NTY3ODkwWA=="),
                MAPPER.writer(Base64Variants.PEM).writeValueAsString(INPUT));
    }

    @Test
    public void testClass() throws Exception
    {
        String result = MAPPER.writeValueAsString(java.util.List.class);
        assertEquals("\"java.util.List\"", result);
    }

    /*
    /**********************************************************
    /* Test methods, array types
    /**********************************************************
     */

    @Test
    public void testLongStringArray() throws Exception
    {
        final int SIZE = 40000;

        StringBuilder sb = new StringBuilder(SIZE*2);
        for (int i = 0; i < SIZE; ++i) {
            sb.append((char) i);
        }
        String str = sb.toString();
        byte[] data = MAPPER.writeValueAsBytes(new String[] { "abc", str, null, str });
        JsonParser p = MAPPER.createParser(data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("abc", p.getString());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        String actual = p.getString();
        assertEquals(str.length(), actual.length());
        assertEquals(str, actual);
        assertToken(JsonToken.VALUE_NULL, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(str, p.getString());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        assertNull(p.nextToken());
        p.close();
    }

    @Test
    public void testIntArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new int[] { 1, 2, 3, -7 });
        assertEquals("[1,2,3,-7]", json);
    }

    @Test
    public void testBigIntArray() throws Exception
    {
        final int SIZE = 99999;
        int[] ints = new int[SIZE];
        for (int i = 0; i < ints.length; ++i) {
            ints[i] = i;
        }

        // Let's try couple of times, to ensure that state is handled
        // correctly by ObjectMapper (wrt buffer recycling used
        // with 'writeAsBytes()')
        for (int round = 0; round < 3; ++round) {
            byte[] data = MAPPER.writeValueAsBytes(ints);
            JsonParser p = MAPPER.createParser(data);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < SIZE; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(i, p.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    @Test
    public void testLongArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE });
        assertEquals("["+Long.MIN_VALUE+",0,"+Long.MAX_VALUE+"]", json);
    }

    @Test
    public void testStringArray() throws Exception
    {
        assertEquals("[\"a\",\"\\\"foo\\\"\",null]",
                MAPPER.writeValueAsString(new String[] { "a", "\"foo\"", null }));
        assertEquals("[]",
                MAPPER.writeValueAsString(new String[] { }));
    }

    @Test
    public void testDoubleArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new double[] { 1.01, 2.0, -7, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }

    @Test
    public void testFloatArray() throws Exception
    {
        String json = MAPPER.writeValueAsString(new float[] { 1.01f, 2.0f, -7f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY });
        assertEquals("[1.01,2.0,-7.0,\"NaN\",\"-Infinity\",\"Infinity\"]", json);
    }

    /*
    /**********************************************************
    /* Test methods, Jackson types
    /**********************************************************
     */

    @Test
    public void testLocation() throws IOException
    {
        File f = new File("/tmp/test.json");
        TokenStreamLocation loc = new TokenStreamLocation(ContentReference.rawReference(f),
                -1, 100, 13);
        Map<String,Object> result = writeAndMap(MAPPER, loc);
        assertEquals(Integer.valueOf(-1), result.get("charOffset"));
        assertEquals(Integer.valueOf(-1), result.get("byteOffset"));
        assertEquals(Integer.valueOf(100), result.get("lineNr"));
        assertEquals(Integer.valueOf(13), result.get("columnNr"));
        assertEquals(4, result.size());
    }

    /**
     * Verify that {@link TokenBuffer} can be properly serialized
     * automatically, using the "standard" JSON sample document
     */
    @Test
    public void testTokenBuffer() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser p = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = TokenBuffer.forGeneration();
        while (p.nextToken() != null) {
            tb.copyCurrentEvent(p);
        }
        p.close();
        // Then serialize as String
        String str = MAPPER.writeValueAsString(tb);
        tb.close();
        // and verify it looks ok
        verifyJsonSpecSampleDoc(createParserUsingReader(str), true);
    }
}

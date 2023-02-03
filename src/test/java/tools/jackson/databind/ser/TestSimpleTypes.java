package tools.jackson.databind.ser;

import tools.jackson.core.Base64Variants;
import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for verifying serialization of simple basic non-structured
 * types; primitives (and/or their wrappers), Strings.
 */
public class TestSimpleTypes
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testBoolean() throws Exception
    {
        assertEquals("true", MAPPER.writeValueAsString(Boolean.TRUE));
        assertEquals("false", MAPPER.writeValueAsString(Boolean.FALSE));
    }

    public void testBooleanArray() throws Exception
    {
        assertEquals("[true,false]", MAPPER.writeValueAsString(new boolean[] { true, false} ));
        assertEquals("[true,false]", MAPPER.writeValueAsString(new Boolean[] { Boolean.TRUE, Boolean.FALSE} ));
    }

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

    public void testShortArray() throws Exception
    {
        assertEquals("[0,1]", MAPPER.writeValueAsString(new short[] { 0, 1 }));
        assertEquals("[2,3]", MAPPER.writeValueAsString(new Short[] { 2, 3 }));
    }

    public void testIntArray() throws Exception
    {
        assertEquals("[0,-3]", MAPPER.writeValueAsString(new int[] { 0, -3 }));
        assertEquals("[13,9]", MAPPER.writeValueAsString(new Integer[] { 13, 9 }));
    }

    /* Note: dealing with floating-point values is tricky; not sure if
     * we can really use equality tests here... JDK does have decent
     * conversions though, to retain accuracy and round-trippability.
     * But still...
     */
    public void testFloat() throws Exception
    {
        double[] values = new double[] {
            0.0, 1.0, 0.1, -37.01, 999.99, 0.3, 33.3, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double d : values) {
           float f = (float) d;
    	   String expected = String.valueOf(f);
           if (Float.isNaN(f) || Float.isInfinite(f)) {
               expected = "\""+expected+"\"";
       	   }
           assertEquals(expected, MAPPER.writeValueAsString(Float.valueOf(f)));
        }
    }

    public void testClass() throws Exception
    {
        String result = MAPPER.writeValueAsString(java.util.List.class);
        assertEquals("\"java.util.List\"", result);
    }
}

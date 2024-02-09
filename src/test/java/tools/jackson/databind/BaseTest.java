package tools.jackson.databind;

import java.io.*;
import java.util.Arrays;

import org.junit.Assert;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;

import tools.jackson.databind.json.JsonMapper;

public abstract class BaseTest
    extends junit.framework.TestCase
{
    /*
    /**********************************************************
    /* Some sample documents:
    /**********************************************************
     */

    protected final static int SAMPLE_SPEC_VALUE_WIDTH = 800;
    protected final static int SAMPLE_SPEC_VALUE_HEIGHT = 600;
    protected final static String SAMPLE_SPEC_VALUE_TITLE = "View from 15th Floor";
    protected final static String SAMPLE_SPEC_VALUE_TN_URL = "http://www.example.com/image/481989943";
    protected final static int SAMPLE_SPEC_VALUE_TN_HEIGHT = 125;
    protected final static String SAMPLE_SPEC_VALUE_TN_WIDTH = "100";
    protected final static int SAMPLE_SPEC_VALUE_TN_ID1 = 116;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID2 = 943;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID3 = 234;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID4 = 38793;

    protected final static String SAMPLE_DOC_JSON_SPEC =
        "{\n"
        +"  \"Image\" : {\n"
        +"    \"Width\" : "+SAMPLE_SPEC_VALUE_WIDTH+",\n"
        +"    \"Height\" : "+SAMPLE_SPEC_VALUE_HEIGHT+","
        +"\"Title\" : \""+SAMPLE_SPEC_VALUE_TITLE+"\",\n"
        +"    \"Thumbnail\" : {\n"
        +"      \"Url\" : \""+SAMPLE_SPEC_VALUE_TN_URL+"\",\n"
        +"\"Height\" : "+SAMPLE_SPEC_VALUE_TN_HEIGHT+",\n"
        +"      \"Width\" : \""+SAMPLE_SPEC_VALUE_TN_WIDTH+"\"\n"
        +"    },\n"
        +"    \"IDs\" : ["+SAMPLE_SPEC_VALUE_TN_ID1+","+SAMPLE_SPEC_VALUE_TN_ID2+","+SAMPLE_SPEC_VALUE_TN_ID3+","+SAMPLE_SPEC_VALUE_TN_ID4+"]\n"
        +"  }"
        +"}"
        ;

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    private static ObjectMapper SHARED_MAPPER;

    protected ObjectMapper sharedMapper() {
        if (SHARED_MAPPER == null) {
            SHARED_MAPPER = newJsonMapper();
        }
        return SHARED_MAPPER;
    }

    protected ObjectMapper objectMapper() {
        return sharedMapper();
    }

    protected ObjectWriter objectWriter() {
        return sharedMapper().writer();
    }

    protected ObjectReader objectReader() {
        return sharedMapper().reader();
    }

    protected ObjectReader objectReader(Class<?> cls) {
        return sharedMapper().readerFor(cls);
    }

    public static JsonMapper newJsonMapper() {
        return new JsonMapper();
    }

    public static JsonMapper.Builder jsonMapperBuilder() {
        return JsonMapper.builder();
    }

    protected static JsonMapper.Builder jsonMapperBuilder(JsonFactory f) {
        return JsonMapper.builder(f);
    }

    /*
    /**********************************************************
    /* Pass-through to remove need for static import
    /**********************************************************
     */

    public static void fail(String msg) { Assert.fail(msg); }

    public static void assertNull(Object v) { Assert.assertNull(v); }
    public static void assertNull(String msg, Object v) { Assert.assertNull(msg, v); }
    public static void assertNotNull(Object v) { Assert.assertNotNull(v); }
    public static void assertNotNull(String msg, Object v) { Assert.assertNotNull(msg, v); }

    public static void assertSame(Object ob1, Object ob2) { Assert.assertSame(ob1, ob2); }
    public static void assertNotSame(Object ob1, Object ob2) { Assert.assertNotSame(ob1, ob2); }

    public static void assertTrue(boolean b) { Assert.assertTrue(b); }
    public static void assertTrue(String msg, boolean b) { Assert.assertTrue(msg, b); }
    public static void assertFalse(boolean b) { Assert.assertFalse(b); }
    public static void assertFalse(String msg, boolean b) { Assert.assertFalse(msg, b); }

    public static void assertEquals(int exp, int act) { Assert.assertEquals(exp, act); }
    public static void assertEquals(String msg, int exp, int act) { Assert.assertEquals(msg, exp, act); }

    public static void assertEquals(double exp, double act, double diff) { Assert.assertEquals(exp, act, diff); }
//    protected static void assertEquals(String msg, double exp, double act) { Assert.assertEquals(msg, exp, act); }

    public static void assertEquals(String exp, String act) { Assert.assertEquals(exp, act); }
    public static void assertEquals(String msg, String exp, String act) { Assert.assertEquals(msg, exp, act); }

    public static void assertEquals(Object exp, Object act) { Assert.assertEquals(exp, act); }
    public static void assertEquals(String msg, Object exp, Object act) { Assert.assertEquals(msg, exp, act); }

    public static void assertArrayEquals(byte[] exp, byte[] act) { Assert.assertArrayEquals(exp, act); }
    public static void assertArrayEquals(String msg, byte[] exp, byte[] act) { Assert.assertArrayEquals(msg, exp, act); }
    public static void assertArrayEquals(char[] exp, char[] act) { Assert.assertArrayEquals(exp, act); }
    public static void assertArrayEquals(int[] exp, int[] act) { Assert.assertArrayEquals(exp, act); }
    public static void assertArrayEquals(long[] exp, long[] act) { Assert.assertArrayEquals(exp, act); }

    public static void assertArrayEquals(Object[] exp, Object[] act) { Assert.assertArrayEquals(exp, act); }

    /*
    /**********************************************************
    /* High-level helpers
    /**********************************************************
     */

    protected void verifyJsonSpecSampleDoc(JsonParser p, boolean verifyContents)
        throws IOException
    {
        verifyJsonSpecSampleDoc(p, verifyContents, true);
    }

    protected void verifyJsonSpecSampleDoc(JsonParser p, boolean verifyContents,
            boolean requireNumbers)
        throws IOException
    {
        if (!p.hasCurrentToken()) {
            p.nextToken();
        }
        assertToken(JsonToken.START_OBJECT, p.currentToken()); // main object

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Image'
        if (verifyContents) {
            verifyFieldName(p, "Image");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'image' object

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Title'
        if (verifyContents) {
            verifyFieldName(p, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(p));
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Thumbnail'
        if (verifyContents) {
            verifyFieldName(p, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Url'
        if (verifyContents) {
            verifyFieldName(p, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(p));
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }
        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(p));
        }

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'IDs'
        assertToken(JsonToken.START_ARRAY, p.nextToken()); // 'ids' array
        verifyIntToken(p.nextToken(), requireNumbers); // ids[0]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID1);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[1]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID2);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[2]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID3);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[3]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID4);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken()); // 'ids' array

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // 'image' object

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // main object
    }

    private void verifyIntToken(JsonToken t, boolean requireNumbers)
    {
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return;
        }
        if (requireNumbers) { // to get error
            assertToken(JsonToken.VALUE_NUMBER_INT, t);
        }
        // if not number, must be String
        if (t != JsonToken.VALUE_STRING) {
            fail("Expected INT or STRING value, got "+t);
        }
    }

    protected void verifyFieldName(JsonParser p, String expName)
        throws IOException
    {
        assertEquals(expName, p.getText());
        assertEquals(expName, p.currentName());
    }

    protected void verifyIntValue(JsonParser p, long expValue)
        throws IOException
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), p.getText());
    }

    /*
    /**********************************************************
    /* Parser/generator construction
    /**********************************************************
     */

    protected JsonParser createParserUsingReader(String input) throws IOException
    {
        return sharedMapper().createParser(new StringReader(input));
    }

    protected JsonParser createParserUsingStream(String input, String encoding)
            throws IOException
    {
        /* 23-Apr-2008, tatus: UTF-32 is not supported by JDK, have to
         *   use our own codec too (which is not optimal since there's
         *   a chance both encoder and decoder might have bugs, but ones
         *   that cancel each other out or such)
         */
        byte[] data;
        if (encoding.equalsIgnoreCase("UTF-32")) {
            data = encodeInUTF32BE(input);
        } else {
            data = input.getBytes(encoding);
        }
        InputStream is = new ByteArrayInputStream(data);
        return sharedMapper().createParser(is);
    }

    /*
    /**********************************************************
    /* JDK ser/deser
    /**********************************************************
     */

    protected static byte[] jdkSerialize(Object o)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(2000);
        try (ObjectOutputStream obOut = new ObjectOutputStream(bytes)) {
            obOut.writeObject(o);
            obOut.close();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T jdkDeserialize(byte[] raw)
    {
        try (ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw))) {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
    /**********************************************************
    /* Additional assertion methods
    /**********************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser p)
    {
        assertToken(expToken, p.currentToken());
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    protected void assertValidLocation(JsonLocation location) {
        assertNotNull("Should have non-null location", location);
        assertTrue("Should have positive line number", location.getLineNr() > 0);
    }

    public static void verifyException(Exception e, Class<?> expType, String expMsg)
        throws Exception
    {
        if (e.getClass() != expType) {
            fail("Expected exception of type "+expType.getName()+", got "+e.getClass().getName());
        }
        if (expMsg != null) {
            verifyException(e, expMsg);
        }
    }

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    public static void verifyException(Throwable e, String... anyMatches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : anyMatches) {
            String lmatch = match.toLowerCase();
            if (lmsg.contains(lmatch)) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("
                +Arrays.asList(anyMatches)+"): got one (of type "+e.getClass().getName()
                +") with message \""+msg+"\"");
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser p)
    {
        // Ok, let's verify other accessors
        int actLen = p.getTextLength();
        char[] ch = p.getTextCharacters();
        String str2 = new String(ch, p.getTextOffset(), actLen);
        String str = p.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.currentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    /*
    /**********************************************************
    /* And other helpers
    /**********************************************************
     */

    protected byte[] encodeInUTF32BE(String input)
    {
        int len = input.length();
        byte[] result = new byte[len * 4];
        int ptr = 0;
        for (int i = 0; i < len; ++i, ptr += 4) {
            char c = input.charAt(i);
            result[ptr] = result[ptr+1] = (byte) 0;
            result[ptr+2] = (byte) (c >> 8);
            result[ptr+3] = (byte) c;
        }
        return result;
    }

    // `static` since 2.16, was only `public` before then.
    public static String q(String str) {
        return '"'+str+'"';
    }

    // `public` since 2.16, was only `protected` before then.
    public static String a2q(String json) {
        return json.replace("'", "\"");
    }
}

package com.fasterxml.jackson.databind;

import java.io.*;
import java.util.Arrays;

import org.junit.Assert;

import com.fasterxml.jackson.core.*;

public abstract class BaseTest
// 19-Sep-2017, tatu: Remove eventually from 3.x, but needs addition of metric ton of `@Test`s
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
    /* Helper classes (beans)
    /**********************************************************
     */
    
    /**
     * Sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    protected static class FiveMinuteUser {
        public enum Gender { MALE, FEMALE };

        public static class Name
        {
            private String _first, _last;

            public Name() { }
            public Name(String f, String l) {
                _first = f;
                _last = l;
            }

            public String getFirst() { return _first; }
            public String getLast() { return _last; }

            public void setFirst(String s) { _first = s; }
            public void setLast(String s) { _last = s; }

            @Override
            public boolean equals(Object o)
            {
                if (o == this) return true;
                if (o == null || o.getClass() != getClass()) return false;
                Name other = (Name) o;
                return _first.equals(other._first) && _last.equals(other._last); 
            }
        }

        private Gender _gender;
        private Name _name;
        private boolean _isVerified;
        private byte[] _userImage;

        public FiveMinuteUser() { }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)
        {
            _name = new Name(first, last);
            _isVerified = verified;
            _gender = g;
            _userImage = data;
        }
        
        public Name getName() { return _name; }
        public boolean isVerified() { return _isVerified; }
        public Gender getGender() { return _gender; }
        public byte[] getUserImage() { return _userImage; }

        public void setName(Name n) { _name = n; }
        public void setVerified(boolean b) { _isVerified = b; }
        public void setGender(Gender g) { _gender = g; }
        public void setUserImage(byte[] b) { _userImage = b; }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            FiveMinuteUser other = (FiveMinuteUser) o;
            if (_isVerified != other._isVerified) return false;
            if (_gender != other._gender) return false; 
            if (!_name.equals(other._name)) return false;
            byte[] otherImage = other._userImage;
            if (otherImage.length != _userImage.length) return false;
            for (int i = 0, len = _userImage.length; i < len; ++i) {
                if (_userImage[i] != otherImage[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    private static ObjectMapper SHARED_MAPPER;

    protected ObjectMapper objectMapper() {
        if (SHARED_MAPPER == null) {
            SHARED_MAPPER = newObjectMapper();
        }
        return SHARED_MAPPER;
    }

    protected ObjectWriter objectWriter() {
        return objectMapper().writer();
    }

    protected ObjectReader objectReader() {
        return objectMapper().reader();
    }
    
    protected ObjectReader objectReader(Class<?> cls) {
        return objectMapper().readerFor(cls);
    }

    protected static ObjectMapper newObjectMapper() {
        return new ObjectMapper();
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

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Image'
        if (verifyContents) {
            verifyFieldName(p, "Image");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'image' object

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_HEIGHT);
        }
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Title'
        if (verifyContents) {
            verifyFieldName(p, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(p));
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Thumbnail'
        if (verifyContents) {
            verifyFieldName(p, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Url'
        if (verifyContents) {
            verifyFieldName(p, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(p));
        }
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }
        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        }
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(p));
        }

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.FIELD_NAME, p.nextToken()); // 'IDs'
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

    protected JsonParser createParserUsingReader(String input)
        throws IOException, JsonParseException
    {
        return SHARED_MAPPER.createParser(new StringReader(input));
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
        return SHARED_MAPPER.createParser(is);
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

    protected void verifyException(Exception e, Class<?> expType, String expMsg)
        throws Exception
    {
        if (e.getClass() != expType) {
            fail("Expected exception of type "+expType.getName()+", got "+e.getClass().getName());
        }
        if (expMsg != null) {
            verifyException(e, expMsg);
        }
    }

    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("
                +Arrays.asList(matches)+"): got one (of type "+e.getClass().getName()
                +") with message \""+msg+"\"");
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser p)
        throws IOException, JsonParseException
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

    public String quote(String str) {
        return '"'+str+'"';
    }
}

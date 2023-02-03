package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonWriteFeature;

import com.fasterxml.jackson.databind.json.JsonMapper;

public class MapperViaParserTest  extends BaseMapTest
{
    final static int TWO_BYTE_ESCAPED = 0x111;
    final static int THREE_BYTE_ESCAPED = 0x1111;

    final static SerializedString TWO_BYTE_ESCAPED_STRING = new SerializedString("&111;");
    final static SerializedString THREE_BYTE_ESCAPED_STRING = new SerializedString("&1111;");

    final static class Pojo
    {
        int _x;

        public void setX(int x) { _x = x; }
    }

    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    /**
     * Trivial simple custom escape definition set.
     */
    static class MyEscapes extends CharacterEscapes
    {
        private static final long serialVersionUID = 1L;

        private final int[] _asciiEscapes;

        public MyEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A"
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // too force "\u0062"
            _asciiEscapes['d'] = CharacterEscapes.ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch)
        {
            if (ch == 'd') {
                return new SerializedString("[D]");
            }
            if (ch == TWO_BYTE_ESCAPED) {
                return TWO_BYTE_ESCAPED_STRING;
            }
            if (ch == THREE_BYTE_ESCAPED) {
                return THREE_BYTE_ESCAPED_STRING;
            }
            return null;
        }
    }

    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    public void testPojoReading() throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        final String JSON = "{ \"x\" : 9 }";
        JsonParser jp = jf.createParser(new StringReader(JSON));

        // let's try first by advancing:
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        Pojo p = jp.readValueAs(Pojo.class);
        assertEquals(9, p._x);
        jp.close();

        // and without
        jp = jf.createParser(new StringReader(JSON));
        p = jp.readValueAs(Pojo.class);
        assertEquals(9, p._x);
        jp.close();
    }

    /**
     * Test similar to above, but instead reads a sequence of values
     */
    public void testIncrementalPojoReading() throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        final String JSON = "[ 1, true, null, \"abc\" ]";
        JsonParser p = jf.createParser(new StringReader(JSON));

        // let's advance past array start to prevent full binding
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(Integer.valueOf(1), p.readValueAs(Integer.class));
        assertEquals(Boolean.TRUE, p.readValueAs(Boolean.class));
        /* note: null can be returned both when there is no more
         * data in current scope, AND when Json null literal is
         * bound!
         */
        assertNull(p.readValueAs(Object.class));
        // but we can verify that it was Json null by:
        assertEquals(JsonToken.VALUE_NULL, p.getLastClearedToken());

        assertEquals("abc", p.readValueAs(String.class));

        // this null is for actually hitting the END_ARRAY
        assertNull(p.readValueAs(Object.class));
        assertEquals(JsonToken.END_ARRAY, p.getLastClearedToken());

        // afrer which there should be nothing to advance to:
        assertNull(p.nextToken());

        p.close();
    }

    @SuppressWarnings("resource")
    public void testPojoReadingFailing() throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory f = new JsonFactory();
        try {
            final String JSON = "{ \"x\" : 9 }";
            JsonParser p = f.createParser(new StringReader(JSON));
            Pojo pojo = p.readValueAs(Pojo.class);
            fail("Expected an exception: got "+pojo);
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }

    public void testEscapingUsingMapper() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
               .configure(JsonWriteFeature.ESCAPE_NON_ASCII, true)
               .build();
        mapper.writeValueAsString(String.valueOf((char) 257));
    }
}

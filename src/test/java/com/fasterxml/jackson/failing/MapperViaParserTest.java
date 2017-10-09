package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @SuppressWarnings("resource")
    public void testPojoReadingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            final String JSON = "{ \"x\" : 9 }";
            JsonParser jp = jf.createParser(new StringReader(JSON));
            Pojo p = jp.readValueAs(Pojo.class);
            fail("Expected an exception: got "+p);
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }
    
    // for [JACKSON-672]
    public void testEscapingUsingMapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
        mapper.writeValueAsString(String.valueOf((char) 257));
    }
}

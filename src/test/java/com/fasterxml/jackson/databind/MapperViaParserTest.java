package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.io.StringReader;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MapperViaParserTest extends BaseMapTest
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

    private final ObjectMapper MAPPER = newObjectMapper();

    @SuppressWarnings("resource")
    public void testPojoReadingOk() throws IOException
    {
        final String JSON = "{ \"x\" : 9 }";
        JsonParser jp = MAPPER.createParser(new StringReader(JSON));
        jp.nextToken();
        Pojo p = jp.readValueAs(Pojo.class);
        assertNotNull(p);
    }

    public void testEscapingUsingMapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(JsonFactory.builder()
                .with(JsonGenerator.Feature.ESCAPE_NON_ASCII).build());
        final String json = mapper.writeValueAsString(String.valueOf((char) 258));
        assertEquals(quote("\\u0102"), json);
    }
}

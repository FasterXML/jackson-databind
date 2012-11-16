package com.fasterxml.jackson.databind;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;

public class TestGeneratorUsingMapper extends BaseMapTest
{
    final static class Pojo
    {
        public int getX() { return 4; }
    }

    /*
    /**********************************************************
    /* Tests for data binding integration
    /**********************************************************
     */

    public void testPojoWriting()
        throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createGenerator(sw);
        gen.writeObject(new Pojo());
        gen.close();
        // trimming needed if main-level object has leading space
        String act = sw.toString().trim();
        assertEquals("{\"x\":4}", act);
    }

    public void testPojoWritingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            StringWriter sw = new StringWriter();
            JsonGenerator gen = jf.createGenerator(sw);
            gen.writeObject(new Pojo());
            gen.close();
            fail("Expected an exception: got sw '"+sw.toString()+"'");
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }

    public void testIssue820() throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (sb.length() <= 5000) {
            sb.append("Yet another line of text...\n");
        }
        String sampleText = sb.toString();
        assertTrue(
                "Sanity check so I don't mess up the sample text later.",
                sampleText.contains("\n"));

        final ObjectMapper mapper = new ObjectMapper();
        final CharacterEscapes defaultCharacterEscapes = new CharacterEscapes() {
            private static final long serialVersionUID = 1L;

            @Override
            public int[] getEscapeCodesForAscii() {
                return standardAsciiEscapesForJSON();
            }

            @Override
            public SerializableString getEscapeSequence(final int ch) {
                return null;
            }
        };

        mapper.getFactory().setCharacterEscapes(defaultCharacterEscapes);
        String jacksonJson = mapper.writeValueAsString(sampleText);
        boolean hasLFs = jacksonJson.indexOf('\n') > 0;
        assertFalse("Should NOT contain linefeeds, should have been escaped", hasLFs);
    }    
}

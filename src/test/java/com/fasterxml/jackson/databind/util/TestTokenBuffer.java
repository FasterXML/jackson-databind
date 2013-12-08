package com.fasterxml.jackson.databind.util;

import java.io.*;
import java.util.UUID;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestTokenBuffer extends com.fasterxml.jackson.test.BaseTest
{
    /*
    /**********************************************************
    /* Basic TokenBuffer tests
    /**********************************************************
     */
    
    /**
     * Test writing of individual simple values
     */
    public void testSimpleWrites() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec

        // First, with empty buffer
        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertNull(jp.nextToken());
        jp.close();

        // Then with simple text
        buf.writeString("abc");

        // First, simple text
        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("abc", jp.getText());
        assertNull(jp.nextToken());
        jp.close();

        // Then, let's append at root level
        buf.writeNumber(13);
        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(13, jp.getIntValue());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();
    }

    public void testSimpleArray() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false); // no ObjectCodec

        // First, empty array
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartArray();
        assertTrue(buf.getOutputContext().inArray());
        buf.writeEndArray();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertTrue(jp.getParsingContext().inArray());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();

        // Then one with simple contents
        buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeBoolean(true);
        buf.writeNull();
        buf.writeEndArray();
        jp = buf.asParser();
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertTrue(jp.getBooleanValue());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();

        // And finally, with array-in-array
        buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeStartArray();
        buf.writeBinary(new byte[3]);
        buf.writeEndArray();
        buf.writeEndArray();
        jp = buf.asParser();
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // TokenBuffer exposes it as embedded object...
        assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, jp.nextToken());
        Object ob = jp.getEmbeddedObject();
        assertNotNull(ob);
        assertTrue(ob instanceof byte[]);
        assertEquals(3, ((byte[]) ob).length);
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();
    }
    
    public void testSimpleObject() throws IOException
    {
        TokenBuffer buf = new TokenBuffer(null, false);

        // First, empty JSON Object
        assertTrue(buf.getOutputContext().inRoot());
        buf.writeStartObject();
        assertTrue(buf.getOutputContext().inObject());
        buf.writeEndObject();
        assertTrue(buf.getOutputContext().inRoot());

        JsonParser jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertTrue(jp.getParsingContext().inObject());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertTrue(jp.getParsingContext().inRoot());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();

        // Then one with simple contents
        buf = new TokenBuffer(null, false);
        buf.writeStartObject();
        buf.writeNumberField("num", 1.25);
        buf.writeEndObject();

        jp = buf.asParser();
        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("num", jp.getCurrentName());
        // and override should also work:
        jp.overrideCurrentName("bah");
        assertEquals("bah", jp.getCurrentName());
        
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(1.25, jp.getDoubleValue());
        // should still have access to (overridden) name
        assertEquals("bah", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        // but not any more
        assertNull(jp.getCurrentName());
        assertNull(jp.nextToken());
        jp.close();
        buf.close();
    }

    /**
     * Verify handling of that "standard" test document (from JSON
     * specification)
     */
    public void testWithJSONSampleDoc() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser jp = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = new TokenBuffer(null, false);
        while (jp.nextToken() != null) {
            tb.copyCurrentEvent(jp);
        }

        // And then request verification; first structure only:
        verifyJsonSpecSampleDoc(tb.asParser(), false);

        // then content check too:
        verifyJsonSpecSampleDoc(tb.asParser(), true);
        tb.close();
        jp.close();
    }

    public void testAppend() throws IOException
    {
        TokenBuffer buf1 = new TokenBuffer(null, false);
        buf1.writeStartObject();
        buf1.writeFieldName("a");
        buf1.writeBoolean(true);
        
        TokenBuffer buf2 = new TokenBuffer(null, false);
        buf2.writeFieldName("b");
        buf2.writeNumber(13);
        buf2.writeEndObject();
        
        buf1.append(buf2);
        
        // and verify that we got it all...
        JsonParser jp = buf1.asParser();
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("b", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(13, jp.getIntValue());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
        buf1.close();
        buf2.close();
    }

    // Since 2.3 had big changes to UUID handling, let's verify we can
    // deal with
    public void testWithUUID() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        for (String value : new String[] {
                "00000007-0000-0000-0000-000000000000",
                "76e6d183-5f68-4afa-b94a-922c1fdb83f8",
                "540a88d1-e2d8-4fb1-9396-9212280d0a7f",
                "2c9e441d-1cd0-472d-9bab-69838f877574",
                "591b2869-146e-41d7-8048-e8131f1fdec5",
                "82994ac2-7b23-49f2-8cc5-e24cf6ed77be",
        }) {
            TokenBuffer buf = new TokenBuffer(mapper, false); // no ObjectCodec
            UUID uuid = UUID.fromString(value);
            mapper.writeValue(buf, uuid);
            buf.close();
    
            // and bring it back
            UUID out = mapper.readValue(buf.asParser(), UUID.class);
            assertEquals(uuid.toString(), out.toString());

            // second part: As per [#362], should NOT use binary with TokenBuffer
            JsonParser jp = buf.asParser();
            assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
            String str = jp.getText();
            assertEquals(value, str);
            jp.close();
        }
    }
    
    /*
    /**********************************************************
    /* Tests to verify interaction of TokenBuffer and JsonParserSequence
    /**********************************************************
     */
    
    public void testWithJsonParserSequenceSimple() throws IOException
    {
        // Let's join a TokenBuffer with JsonParser first
        TokenBuffer buf = new TokenBuffer(null, false);
        buf.writeStartArray();
        buf.writeString("test");
        JsonParser jp = createParserUsingReader("[ true, null ]");
        
        JsonParserSequence seq = JsonParserSequence.createFlattened(buf.asParser(), jp);
        assertEquals(2, seq.containedParsersCount());

        assertFalse(jp.isClosed());
        
        assertFalse(seq.hasCurrentToken());
        assertNull(seq.getCurrentToken());
        assertNull(seq.getCurrentName());

        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_STRING, seq.nextToken());
        assertEquals("test", seq.getText());
        // end of first parser input, should switch over:
        
        assertToken(JsonToken.START_ARRAY, seq.nextToken());
        assertToken(JsonToken.VALUE_TRUE, seq.nextToken());
        assertToken(JsonToken.VALUE_NULL, seq.nextToken());
        assertToken(JsonToken.END_ARRAY, seq.nextToken());

        /* 17-Jan-2009, tatus: At this point, we may or may not get an
         *   exception, depending on how underlying parsers work.
         *   Ideally this should be fixed, probably by asking underlying
         *   parsers to disable checking for balanced start/end markers.
         */

        // for this particular case, we won't get an exception tho...
        assertNull(seq.nextToken());
        // not an error to call again...
        assertNull(seq.nextToken());

        // also: original parsers should be closed
        assertTrue(jp.isClosed());
        jp.close();
        buf.close();
        seq.close();
    }
    
    /**
     * Test to verify that TokenBuffer and JsonParserSequence work together
     * as expected.
     */
    @SuppressWarnings("resource")
    public void testWithMultipleJsonParserSequences() throws IOException
    {
        TokenBuffer buf1 = new TokenBuffer(null, false);
        buf1.writeStartArray();
        TokenBuffer buf2 = new TokenBuffer(null, false);
        buf2.writeString("a");
        TokenBuffer buf3 = new TokenBuffer(null, false);
        buf3.writeNumber(13);
        TokenBuffer buf4 = new TokenBuffer(null, false);
        buf4.writeEndArray();

        JsonParserSequence seq1 = JsonParserSequence.createFlattened(buf1.asParser(), buf2.asParser());
        assertEquals(2, seq1.containedParsersCount());
        JsonParserSequence seq2 = JsonParserSequence.createFlattened(buf3.asParser(), buf4.asParser());
        assertEquals(2, seq2.containedParsersCount());
        JsonParserSequence combo = JsonParserSequence.createFlattened(seq1, seq2);
        // should flatten it to have 4 underlying parsers
        assertEquals(4, combo.containedParsersCount());

        assertToken(JsonToken.START_ARRAY, combo.nextToken());
        assertToken(JsonToken.VALUE_STRING, combo.nextToken());
        assertEquals("a", combo.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, combo.nextToken());
        assertEquals(13, combo.getIntValue());
        assertToken(JsonToken.END_ARRAY, combo.nextToken());
        assertNull(combo.nextToken());        
        buf1.close();
        buf2.close();
        buf3.close();
        buf4.close();
    }    
}

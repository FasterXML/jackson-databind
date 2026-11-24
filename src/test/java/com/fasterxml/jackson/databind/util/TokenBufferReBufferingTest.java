package com.fasterxml.jackson.databind.util;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6482]: re-buffering content read from a {@link TokenBuffer}
 * into another {@link TokenBuffer} should not copy structured values.
 */
@SuppressWarnings("resource")
public class TokenBufferReBufferingTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void copyValueFromBufferIsNotCopied() throws Exception
    {
        TokenBuffer src = _bufferOf(_largeArray(1000));
        JsonParser p = src.asParser();
        assertToken(JsonToken.START_ARRAY, p.nextToken());

        TokenBuffer copy = new TokenBuffer(p);
        copy.copyCurrentStructure(p);
        // Parser must point to the end of value, as with actual copying
        assertToken(JsonToken.END_ARRAY, p.currentToken());
        // with same context state as when skipping
        JsonParser p2 = src.asParser();
        p2.nextToken();
        p2.skipChildren();
        assertEquals(p2.getParsingContext().getEntryCount(), p.getParsingContext().getEntryCount());
        assertEquals(p2.getParsingContext().getTypeDesc(), p.getParsingContext().getTypeDesc());
        assertNull(p.nextToken());

        // Only one entry (reference) in the copy
        assertSame(copy._first, copy._last);
        assertEquals(1, copy._appendAt);

        assertEquals(_largeArray(1000), _toJson(copy));
        assertEquals(_largeArray(1000), _readAsJson(copy.asParser()));
        assertEquals(JsonToken.START_ARRAY, copy.firstToken());
    }

    // Chain of re-buffering, as with nested polymorphic values
    @Test
    public void nestedReBuffering() throws Exception
    {
        final int DEPTH = 50;
        final String leaf = _largeArray(500);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DEPTH; ++i) {
            sb.append("{\"c\":[");
        }
        sb.append(leaf);
        for (int i = 0; i < DEPTH; ++i) {
            sb.append("],\"t\":").append(i).append('}');
        }
        final String json = sb.toString();

        TokenBuffer buffer = _bufferOf(json);
        JsonParser p = buffer.asParser();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        for (int level = 0; level < DEPTH; ++level) {
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("c", p.currentName());
            // Buffer the property (name + value) like type deserializers do
            TokenBuffer tb = new TokenBuffer(p);
            tb.copyCurrentStructure(p);
            assertToken(JsonToken.END_ARRAY, p.currentToken());
            assertEquals("t", p.nextFieldName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(DEPTH - 1 - level, p.getIntValue());
            assertToken(JsonToken.END_OBJECT, p.nextToken());

            // Name + reference: no copying of tokens
            assertSame(tb._first, tb._last);
            assertEquals(2, tb._appendAt);

            // and then continue with buffered content
            p = tb.asParser();
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            if (level < DEPTH - 1) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
            }
        }
        // and at the end, the leaf array
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        TokenBuffer last = new TokenBuffer(p);
        last.copyCurrentStructure(p);
        assertEquals(leaf, _toJson(last));
    }

    // Reference to a range that itself contains references
    @Test
    public void referenceContainingReferences() throws Exception
    {
        TokenBuffer src = _bufferOf(a2q("{'a':{'x':[1,2,{'y':true}]},'b':'end'}"));
        JsonParser p = src.asParser();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        // Second buffer: explicitly written tokens and a reference to value of "a"
        TokenBuffer tb2 = new TokenBuffer(p);
        tb2.writeStartArray();
        tb2.writeString("first");
        tb2.copyCurrentStructure(p);
        tb2.writeNumber(42);
        tb2.writeEndArray();
        assertEquals(a2q("['first',{'x':[1,2,{'y':true}]},42]"), _toJson(tb2));

        // Then third buffer referring to range of second one
        JsonParser p2 = tb2.asParser();
        assertToken(JsonToken.START_ARRAY, p2.nextToken());
        TokenBuffer tb3 = new TokenBuffer(p2);
        tb3.copyCurrentStructure(p2);
        assertNull(p2.nextToken());
        assertSame(tb3._first, tb3._last);
        assertEquals(1, tb3._appendAt);
        assertEquals(a2q("['first',{'x':[1,2,{'y':true}]},42]"), _toJson(tb3));
        assertEquals(a2q("['first',{'x':[1,2,{'y':true}]},42]"), _readAsJson(tb3.asParser()));

        // As well as "append()" which uses copying
        TokenBuffer tb4 = new TokenBuffer(MAPPER, false);
        tb4.append(tb3);
        tb4.append(tb2);
        JsonParser p4 = tb4.asParser();
        assertEquals(a2q("['first',{'x':[1,2,{'y':true}]},42]"), _readAsJson(p4));
        assertEquals(a2q("['first',{'x':[1,2,{'y':true}]},42]"), _readAsJson(p4));
        assertNull(p4.nextToken());
    }

    // Accessors that look ahead must not look past end of referenced range
    @Test
    public void lookAheadAtEndOfRange() throws Exception
    {
        // Source has FIELD_NAME right after the value to reference
        TokenBuffer src = _bufferOf(a2q("{'a':{'b':1},'zzz':2}"));
        JsonParser p = src.asParser();
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        TokenBuffer tb = new TokenBuffer(p);
        tb.writeStartObject();
        tb.writeFieldName("ref");
        tb.copyCurrentStructure(p);
        tb.writeFieldName("next");
        tb.writeNumber(3);
        tb.writeEndObject();

        JsonParser p2 = tb.asParser();
        assertToken(JsonToken.START_OBJECT, p2.nextToken());
        assertEquals("ref", p2.nextFieldName());
        assertToken(JsonToken.START_OBJECT, ((TokenBuffer.Parser) p2).peekNextToken());
        assertToken(JsonToken.START_OBJECT, p2.nextToken());
        assertEquals("b", p2.nextFieldName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertToken(JsonToken.END_OBJECT, ((TokenBuffer.Parser) p2).peekNextToken());
        assertToken(JsonToken.END_OBJECT, p2.nextToken());
        // at end of range: next is from the referring buffer, not from source
        assertToken(JsonToken.FIELD_NAME, ((TokenBuffer.Parser) p2).peekNextToken());
        assertEquals("next", p2.nextFieldName());
        assertEquals("next", p2.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p2.nextToken());
        assertEquals(3, p2.getIntValue());
        assertToken(JsonToken.END_OBJECT, p2.nextToken());
        assertNull(((TokenBuffer.Parser) p2).peekNextToken());
        assertNull(p2.nextToken());

        // and skipping over reference too
        p2 = tb.asParser();
        assertToken(JsonToken.START_OBJECT, p2.nextToken());
        assertEquals("ref", p2.nextFieldName());
        assertToken(JsonToken.START_OBJECT, p2.nextToken());
        p2.skipChildren();
        assertToken(JsonToken.END_OBJECT, p2.currentToken());
        assertEquals("next", p2.nextFieldName());
    }

    // Reading via `JsonParserSequence` (as done by type deserializers)
    @Test
    public void copyFromSequence() throws Exception
    {
        TokenBuffer first = _bufferOf(a2q("{'a':[1,2,3]}"));
        JsonParser p = JsonParserSequence.createFlattened(false,
                first.asParser(), MAPPER.createParser(a2q("{'b':[4]}")));
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        TokenBuffer tb = new TokenBuffer(p);
        tb.copyCurrentStructure(p);
        // reference to buffered content
        assertSame(tb._first, tb._last);
        assertEquals(1, tb._appendAt);
        // and then actual copy from the JSON parser
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        tb.copyCurrentStructure(p);
        assertNull(p.nextToken());

        JsonParser p2 = tb.asParser();
        assertEquals(a2q("{'a':[1,2,3]}"), _readAsJson(p2));
        assertEquals(a2q("{'b':[4]}"), _readAsJson(p2));
        assertNull(p2.nextToken());
    }

    // If buffered content is not complete value, must copy as before
    @Test
    public void unbalancedContentIsCopied() throws Exception
    {
        TokenBuffer first = new TokenBuffer(MAPPER, false);
        first.writeStartObject();
        first.writeFieldName("a");
        first.writeNumber(1);
        TokenBuffer second = new TokenBuffer(MAPPER, false);
        second.writeFieldName("b");
        second.writeNumber(2);
        second.writeEndObject();

        JsonParser p = JsonParserSequence.createFlattened(false,
                first.asParser(), second.asParser());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        TokenBuffer tb = new TokenBuffer(p);
        tb.copyCurrentStructure(p);
        assertToken(JsonToken.END_OBJECT, p.currentToken());
        assertNull(p.nextToken());
        assertEquals(a2q("{'a':1,'b':2}"), _toJson(tb));
    }

    // Native ids are retained, and not attached to following tokens
    @Test
    public void nativeIdsRetained() throws Exception
    {
        TokenBuffer src = new TokenBuffer(MAPPER, true);
        src.writeStartArray();
        src.writeObjectId("obj1");
        src.writeTypeId("type1");
        src.writeStartObject();
        src.writeFieldName("a");
        src.writeNumber(1);
        src.writeEndObject();
        src.writeEndArray();

        JsonParser p = src.asParser();
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertEquals("obj1", p.getObjectId());

        TokenBuffer tb = new TokenBuffer(p);
        assertTrue(tb.canWriteObjectId());
        tb.writeStartArray();
        tb.copyCurrentStructure(p);
        tb.writeString("after");
        tb.writeEndArray();

        JsonParser p2 = tb.asParser();
        assertToken(JsonToken.START_ARRAY, p2.nextToken());
        assertToken(JsonToken.START_OBJECT, p2.nextToken());
        assertEquals("obj1", p2.getObjectId());
        assertEquals("type1", p2.getTypeId());
        // Ids of contents must be same as in source
        JsonParser srcP = src.asParser();
        assertToken(JsonToken.START_ARRAY, srcP.nextToken());
        assertToken(JsonToken.START_OBJECT, srcP.nextToken());
        for (int i = 0; i < 3; ++i) {
            assertToken(srcP.nextToken(), p2.nextToken());
            assertEquals(srcP.getObjectId(), p2.getObjectId());
            assertEquals(srcP.getTypeId(), p2.getTypeId());
        }
        assertToken(JsonToken.END_OBJECT, p2.currentToken());
        // but not attached to tokens written after copying
        assertToken(JsonToken.VALUE_STRING, p2.nextToken());
        assertNull(p2.getObjectId());
        assertNull(p2.getTypeId());
        assertToken(JsonToken.END_ARRAY, p2.nextToken());
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private TokenBuffer _bufferOf(String json) throws Exception {
        try (JsonParser p = MAPPER.createParser(json)) {
            p.nextToken();
            TokenBuffer tb = new TokenBuffer(p);
            tb.copyCurrentStructure(p);
            return tb;
        }
    }

    private String _toJson(TokenBuffer tb) throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            tb.serialize(g);
        }
        return sw.toString();
    }

    // Reads next value from parser and returns it as JSON
    private String _readAsJson(JsonParser p) throws Exception {
        StringWriter sw = new StringWriter();
        p.nextToken();
        try (JsonGenerator g = MAPPER.createGenerator(sw)) {
            g.copyCurrentStructure(p);
        }
        return sw.toString();
    }

    private static String _largeArray(int count) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        return sb.append(']').toString();
    }
}

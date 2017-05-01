package com.fasterxml.jackson.databind.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserSequence;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParserSequenceTest extends BaseMapTest {

    private final ObjectMapper MAPPER = objectMapper();

    /**
     * Verifies fix for [core#372]
     */
    @SuppressWarnings("resource")
    public void testJsonParserSequenceOverridesSkipChildren() throws Exception
    {
        // Create parser from TokenBuffer containing an incomplete JSON object
        TokenBuffer buf1 = new TokenBuffer(MAPPER, false);
        buf1.writeStartObject();
        buf1.writeFieldName("foo");
        buf1.writeStartObject();
        JsonParser parser1 = buf1.asParser();

        // Create parser from second TokenBuffer that completes the object started by the first buffer
        TokenBuffer buf2 = new TokenBuffer(MAPPER, false);
        buf2.writeEndObject();
        buf2.writeEndObject();
        JsonParser parser2 = buf2.asParser();

        // Create sequence of both parsers and verify tokens
        JsonParser parserSequence = JsonParserSequence.createFlattened(false, parser1, parser2);
        assertToken(JsonToken.START_OBJECT, parserSequence.nextToken());
        assertToken(JsonToken.FIELD_NAME, parserSequence.nextToken());
        assertToken(JsonToken.START_OBJECT, parserSequence.nextToken());

        // Skip children of current token. JsonParserSequence's overridden version should switch to the next parser
        // in the sequence
        parserSequence.skipChildren();

        // Verify last token
        assertToken(JsonToken.END_OBJECT, parserSequence.nextToken());
    }
}

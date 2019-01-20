package com.fasterxml.jackson.databind.node;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import com.fasterxml.jackson.databind.*;

/**
 * Tests to verify handling of empty content with "readTree()"
 */
public class EmptyContentAsTreeTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = objectMapper();

    // [databind#1406]: when passing `JsonParser`, indicate lack of content
    // by returning `null`

    public void testNullFromEOFWithParser() throws Exception
    {
        assertNull(MAPPER.readTree(new StringReader("")));
        assertNull(MAPPER.readTree(new ByteArrayInputStream(new byte[0])));
    }

    // [databind#1406]
    public void testNullFromEOFWithParserViaReader() throws Exception
    {
        assertNull(MAPPER.readTree(new StringReader("")));
        assertNull(MAPPER.readTree(new ByteArrayInputStream(new byte[0])));
        assertNull(MAPPER.readerFor(JsonNode.class)
                .readTree(new StringReader("")));
        assertNull(MAPPER.readerFor(JsonNode.class)
                .readTree(new ByteArrayInputStream(new byte[0])));
    }

    // [databind#2211]: when passing content sources OTHER than `JsonParser`,
    // return "missing node" instead of alternate (return `null`, throw exception).
    public void testMissingNodeForEOFOther() throws Exception
    {
        
    }

    public void testMissingNodeForEOFOtherViaReader() throws Exception
    {
        
    }
}

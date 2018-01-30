package com.fasterxml.jackson.databind.misc;

import java.io.*;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * Unit test mostly written to cover issue with unintended blocking
 * after data binding.
 */
public class TestBlocking extends BaseMapTest
{
    /**
     * This is an indirect test that should trigger problems if (and only if)
     * underlying parser is advanced beyond the only element array.
     * Basically, although content is invalid, this should be encountered
     * quite yet.
     */
    public void testEagerAdvance() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = createParserUsingReader("[ 1  ");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        // And then try to map just a single entry: shouldn't fail:
        Integer I = mapper.readValue(jp, Integer.class);
        assertEquals(Integer.valueOf(1), I);

        // and should fail only now:
        try {
            jp.nextToken();
        } catch (IOException ioe) {
            verifyException(ioe, "Unexpected end-of-input: expected close marker for ARRAY");
        }
        jp.close();
    }
}

package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.*;

public class TestJsonPointer
    extends BaseMapTest
{
    public void testIt() throws Exception
    {
        final JsonNode SAMPLE_ROOT = objectMapper().readTree(SAMPLE_DOC_JSON_SPEC);
        
        // first: "empty" pointer points to context node:
        assertSame(SAMPLE_ROOT, SAMPLE_ROOT.at(JsonPointer.compile("")));

        // then simple reference
        assertTrue(SAMPLE_ROOT.at(JsonPointer.compile("/Image")).isObject());

        JsonNode n = SAMPLE_ROOT.at(JsonPointer.compile("/Image/Width"));
        assertTrue(n.isNumber());
        assertEquals(SAMPLE_SPEC_VALUE_WIDTH, n.asInt());

        assertEquals(SAMPLE_SPEC_VALUE_HEIGHT,
                SAMPLE_ROOT.at(JsonPointer.compile("/Image/Height")).asInt());
    }
}

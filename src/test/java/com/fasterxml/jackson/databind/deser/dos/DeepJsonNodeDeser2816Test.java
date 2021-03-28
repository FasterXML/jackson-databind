package com.fasterxml.jackson.databind.deser.dos;

import com.fasterxml.jackson.databind.*;

// [databind#2816], wrt JsonNode
public class DeepJsonNodeDeser2816Test extends BaseMapTest
{
    // 28-Mar-2021, tatu: Used to fail at 5000 for tree/object,
    // 8000 for tree/array, before work on iterative JsonNode deserializer
    // ... currently gets a bit slow at 1M but passes
//    private final static int TOO_DEEP_NESTING = 1_000_000;
    private final static int TOO_DEEP_NESTING = 9999;

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testTreeWithArray() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        JsonNode n = MAPPER.readTree(doc);
        assertTrue(n.isArray());
    }

    public void testTreeWithObject() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        JsonNode n = MAPPER.readTree(doc);
        assertTrue(n.isObject());
    }
    
    private String _nestedDoc(int nesting, String open, String close) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}

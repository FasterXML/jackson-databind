package com.fasterxml.jackson.failing;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.*;

// [databind#2816]
public class DeepNestingDeser2816Test extends BaseMapTest
{
    // 2000 passes for all; 3000 fails for untyped, 5000 for tree/object,
    // 8000 for tree/array too
    private final static int TOO_DEEP_NESTING = 8000;

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testUntypedWithArray() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof List<?>);
    }

    public void testUntypedWithObject() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof Map<?,?>);
    }

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

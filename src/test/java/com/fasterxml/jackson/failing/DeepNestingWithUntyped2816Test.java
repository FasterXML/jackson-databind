package com.fasterxml.jackson.failing;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.*;

// [databind#2816]
public class DeepNestingWithUntyped2816Test extends BaseMapTest
{
    // 2000 passes, 3000 fails
    private final static int TOO_DEEP_NESTING = 4000;

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testWithArray() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof List<?>);
    }

    public void testWithObject() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof Map<?,?>);
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

package com.fasterxml.jackson.failing;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.*;

// [databind#2816] wrt "untyped" (Maps, Lists)
public class DeepNestingUntypedDeser2816Test extends BaseMapTest
{
    // 28-Mar-2021, tatu: Currently 3000 fails for untyped/Object,
    //     4000 for untyped/Array
    private final static int TOO_DEEP_NESTING = 4000;

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

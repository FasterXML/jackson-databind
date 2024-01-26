package com.fasterxml.jackson.databind.deser.dos;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#3397], wrt JsonNode
public class DeepJsonNodeDeser3397Test
{
    // 28-Mar-2021, tatu: Used to fail at 5000 for tree/object,
    // 8000 for tree/array, before work on iterative JsonNode deserializer
    // ... currently gets a bit slow at 1M but passes.
    // But test with 100k as practical limit, to guard against regression
//    private final static int TOO_DEEP_NESTING = 1_000_000;
    private final static int TOO_DEEP_NESTING = StreamReadConstraints.DEFAULT_MAX_DEPTH * 10;

    private final JsonFactory jsonFactory = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper MAPPER = JsonMapper.builder(jsonFactory).build();

    @Test
    public void testTreeWithArray() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        JsonNode n = MAPPER.readTree(doc);
        assertTrue(n.isArray());
    }

    @Test
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

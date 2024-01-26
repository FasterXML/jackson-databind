package com.fasterxml.jackson.databind.deser.dos;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

// For [databind#2816] / [databind#3473]
public class DeepNestingUntypedDeserTest
{
    // 28-Mar-2021, tatu: Currently 3000 fails for untyped/Object,
    //     4000 for untyped/Array
    // 31-May-2022, tatu: But no more! Can handle much much larger
    //   nesting levels, bounded by memory usage not stack. Tested with
    //   1 million (!) nesting levels, but to keep tests fast use 100k
    private final static int TOO_DEEP_NESTING = StreamReadConstraints.DEFAULT_MAX_DEPTH * 100;

    private final JsonFactory jsonFactory = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper MAPPER = JsonMapper.builder(jsonFactory).build();


    @Test
    public void testFormerlyTooDeepUntypedWithArray() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof List<?>);

        // ... but also work with Java array
        ob = MAPPER.readerFor(Object.class)
                .with(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY)
                .readValue(doc, Object.class);
        assertTrue(ob instanceof Object[]);
    }

    @Test
    public void testFormerlyTooDeepUntypedWithObject() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        Object ob = MAPPER.readValue(doc, Object.class);
        assertTrue(ob instanceof Map<?, ?>);
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

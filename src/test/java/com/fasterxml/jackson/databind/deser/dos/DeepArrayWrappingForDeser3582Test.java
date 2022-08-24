package com.fasterxml.jackson.databind.deser.dos;

import com.fasterxml.jackson.databind.*;

public class DeepArrayWrappingForDeser3582Test extends BaseMapTest
{
    // 23-Aug-2022, tatu: Before fix, fails with 5000
    //    (but passes with 2000)
//    private final static int TOO_DEEP_NESTING = 4999;
    private final static int TOO_DEEP_NESTING = 999;

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            .build();

    public void testArrayWrapping() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "{}");
        Point p = MAPPER.readValue(doc, Point.class);
        assertNotNull(p);
    }

    private String _nestedDoc(int nesting, String open, String close, String content) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        sb.append("\n").append(content).append("\n");
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}

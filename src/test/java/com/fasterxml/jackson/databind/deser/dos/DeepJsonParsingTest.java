package com.fasterxml.jackson.databind.deser.dos;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class DeepJsonParsingTest extends BaseMapTest
{
    private final static int TOO_DEEP_NESTING = 2_000;

    private final JsonFactory unconstrainedFactory = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
            .build();
    private final ObjectMapper unconstrainedMapper = JsonMapper.builder(unconstrainedFactory).build();
    private final ObjectMapper defaultMapper = JsonMapper.builder().build();

    public void testParseWithArrayWithDefaultConfig() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        try (JsonParser jp = defaultMapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");

        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
    }

    public void testParseWithObjectWithDefaultConfig() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        try (JsonParser jp = defaultMapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
    }

    public void testParseWithArrayWithUnconstrainedConfig() throws Exception
    {
        final String doc = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ");
        try (JsonParser jp = unconstrainedMapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
        }
    }

    public void testParseWithObjectWithUnconstrainedConfig() throws Exception
    {
        final String doc = "{"+_nestedDoc(TOO_DEEP_NESTING, "\"x\":{", "} ") + "}";
        try (JsonParser jp = unconstrainedMapper.createParser(doc)) {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
        }
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

package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

// Tests for new (2.14) `JsonNodeFeature`
public class NodeFeaturesTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader READER = MAPPER.reader();

    private final ObjectNode DOC_EMPTY = MAPPER.createObjectNode();
    private final ObjectNode DOC_WITH_NULL = MAPPER.createObjectNode();
    {
        DOC_WITH_NULL.putNull("nvl");
    }
    private final String JSON_WITH_NULL = a2q("{'nvl':null}");

    public void testDefaultSettings() throws Exception
    {
        assertTrue(READER.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));

        assertFalse(READER.without(JsonNodeFeature.READ_NULL_PROPERTIES)
                .isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
    }

    public void testReadNulls() throws Exception
    {
        // so by default we'll get null included
        assertEquals(DOC_WITH_NULL, READER.readTree(JSON_WITH_NULL));

        ObjectMapper noNullsMapper = JsonMapper.builder()
                .disable(JsonNodeFeature.READ_NULL_PROPERTIES)
                .build();
        ObjectReader r = noNullsMapper.reader();
        assertFalse(r.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertEquals(DOC_EMPTY, r.readTree(JSON_WITH_NULL));

        // but also verify we can "reset" reader's behavior
        ObjectReader r2 = r.with(JsonNodeFeature.READ_NULL_PROPERTIES);
        assertEquals(DOC_WITH_NULL, r2.readTree(JSON_WITH_NULL));

        // and then bit more complex doc
        ObjectNode exp = noNullsMapper.createObjectNode();
        exp.put("a", 1);
        exp.put("c", true);
        assertEquals(exp, r.readTree(a2q("{'a':1,'b':null,'c':true}")));
    }
}

package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.DatatypeFeatures;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Tests for new (2.14) `JsonNodeFeature`
public class NodeFeaturesTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader READER = MAPPER.reader();
    private final ObjectWriter WRITER = MAPPER.writer();

    private final ObjectNode DOC_EMPTY = MAPPER.createObjectNode();
    private final ObjectNode DOC_WITH_NULL = MAPPER.createObjectNode();
    {
        DOC_WITH_NULL.putNull("nvl");
    }
    private final String JSON_EMPTY = ("{}");
    private final String JSON_WITH_NULL = a2q("{'nvl':null}");

    @Test
    public void testDefaultSettings() throws Exception
    {
        assertTrue(READER.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(READER.without(JsonNodeFeature.READ_NULL_PROPERTIES)
                .isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));

        assertTrue(READER.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES));
        assertFalse(READER.without(JsonNodeFeature.WRITE_NULL_PROPERTIES)
                .isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES));
    }

    @Test
    public void testImplicitVsExplicit()
    {
        DatatypeFeatures dfs = DatatypeFeatures.defaultFeatures();
        assertTrue(dfs.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(dfs.isExplicitlySet(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(dfs.isExplicitlyEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(dfs.isExplicitlyDisabled(JsonNodeFeature.READ_NULL_PROPERTIES));

        // disable
        dfs = dfs.without(JsonNodeFeature.READ_NULL_PROPERTIES);
        assertFalse(dfs.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertTrue(dfs.isExplicitlySet(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(dfs.isExplicitlyEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertTrue(dfs.isExplicitlyDisabled(JsonNodeFeature.READ_NULL_PROPERTIES));

        // re-enable
        dfs = dfs.with(JsonNodeFeature.READ_NULL_PROPERTIES);
        assertTrue(dfs.isEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertTrue(dfs.isExplicitlySet(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertTrue(dfs.isExplicitlyEnabled(JsonNodeFeature.READ_NULL_PROPERTIES));
        assertFalse(dfs.isExplicitlyDisabled(JsonNodeFeature.READ_NULL_PROPERTIES));
    }

    /*
    /**********************************************************************
    /* ObjectNode property handling: null-handling
    /**********************************************************************
     */

    // [databind#3421]
    @Test
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

    // [databind#3476]
    @Test
    public void testWriteNulls() throws Exception
    {
        // so by default we'll get null written
        assertEquals(JSON_WITH_NULL, WRITER.writeValueAsString(DOC_WITH_NULL));

        ObjectMapper noNullsMapper = JsonMapper.builder()
                .disable(JsonNodeFeature.WRITE_NULL_PROPERTIES)
                .build();
        ObjectWriter w = noNullsMapper.writer();
        assertFalse(w.isEnabled(JsonNodeFeature.WRITE_NULL_PROPERTIES));
        assertEquals(JSON_EMPTY, w.writeValueAsString(DOC_WITH_NULL));

        // but also verify we can "reset" writer's behavior
        ObjectWriter w2 = w.with(JsonNodeFeature.WRITE_NULL_PROPERTIES);
        assertEquals(JSON_WITH_NULL, w2.writeValueAsString(DOC_WITH_NULL));

        // and then bit more complex doc
        ObjectNode doc = noNullsMapper.createObjectNode();
        doc.put("a", 1);
        doc.putNull("b");
        doc.put("c", true);
        assertEquals(a2q("{'a':1,'c':true}"), w.writeValueAsString(doc));
    }

    /*
    /**********************************************************************
    /* ObjectNode property handling: sorting on write
    /**********************************************************************
     */

    // [databind#3476]
    @Test
    public void testWriteSortedProperties() throws Exception
    {
        assertFalse(WRITER.isEnabled(JsonNodeFeature.WRITE_PROPERTIES_SORTED));

        ObjectNode doc = MAPPER.createObjectNode();
        doc.put("b", 2);
        doc.put("c", 3);
        doc.put("a", 1);

        // by default, retain insertion order:
        assertEquals(a2q("{'b':2,'c':3,'a':1}"), WRITER.writeValueAsString(doc));

        // but if forcing sorting, changes
        final String SORTED = a2q("{'a':1,'b':2,'c':3}");
        ObjectMapper sortingMapper = JsonMapper.builder()
                .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED)
                .build();
        assertEquals(SORTED, sortingMapper.writeValueAsString(doc));

        // Let's verify ObjectWriter config too
        ObjectWriter w2 = WRITER.with(JsonNodeFeature.WRITE_PROPERTIES_SORTED);
        assertTrue(w2.isEnabled(JsonNodeFeature.WRITE_PROPERTIES_SORTED));
        assertEquals(SORTED, w2.writeValueAsString(doc));
    }

    /*
    /**********************************************************************
    /* Other features
    /**********************************************************************
     */
}

package com.fasterxml.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5542]
 * Test to verify that {@link ObjectReader} properly handles
 * {@link DeserializationFeature#FAIL_ON_UNRESOLVED_OBJECT_IDS}
 * when using {@code readValue(JsonParser)}.
 */
public class ObjectIdWithReader5542Test extends DatabindTestUtil
{
    static class Wrapper {
        public ValueNode node;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class ValueNode {
        public int value;
        public Wrapper next;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test that ObjectMapper.readValue() correctly throws exception for unresolved object IDs
     * when FAIL_ON_UNRESOLVED_OBJECT_IDS is enabled (default).
     */
    @Test
    public void testObjectMapperFailsOnUnresolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        try {
            MAPPER.readValue(json, Wrapper.class);
            fail("Should have thrown UnresolvedForwardReference");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference");
        }
    }

    /**
     * Test that ObjectReader.readValue(JsonParser) also throws exception for unresolved object IDs
     * when FAIL_ON_UNRESOLVED_OBJECT_IDS is enabled (default).
     *
     * This is the bug: ObjectReader.readValue(JsonParser) doesn't call checkUnresolvedObjectId()
     */
    @Test
    public void testObjectReaderFailsOnUnresolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        ObjectReader reader = MAPPER.readerFor(Wrapper.class);
        try (JsonParser p = MAPPER.createParser(json)) {
            reader.readValue(p);
            fail("Should have thrown UnresolvedForwardReference");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference");
        }
    }

    /**
     * Test that ObjectReader with disabled FAIL_ON_UNRESOLVED_OBJECT_IDS
     * returns null for unresolved object IDs.
     */
    @Test
    public void testObjectReaderWithDisabledFeature() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        ObjectReader reader = MAPPER.readerFor(Wrapper.class)
                .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);

        try (JsonParser p = MAPPER.createParser(json)) {
            Wrapper wrapper = reader.readValue(p);
            assertNotNull(wrapper);
            assertNotNull(wrapper.node);
            assertNull(wrapper.node.next.node); // Unresolved reference should be null
        }
    }

    /**
     * Test that ObjectReader works correctly when all object IDs are resolved.
     */
    @Test
    public void testObjectReaderWithResolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':1}}}");

        ObjectReader reader = MAPPER.readerFor(Wrapper.class);
        try (JsonParser p = MAPPER.createParser(json)) {
            Wrapper wrapper = reader.readValue(p);
            assertNotNull(wrapper);
            assertNotNull(wrapper.node);
            assertSame(wrapper.node, wrapper.node.next.node); // Should be same object
        }
    }
}

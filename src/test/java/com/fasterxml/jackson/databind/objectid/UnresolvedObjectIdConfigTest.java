package com.fasterxml.jackson.databind.objectid;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;

/**
 * Unit test to verify handling of Object Id deserialization in
 * conjunction with {@link DeserializationFeature#FAIL_ON_UNRESOLVED_OBJECT_IDS}.
 */
public class UnresolvedObjectIdConfigTest extends BaseMapTest {

    /*
    /**********************************************************
    /* Set Up
    /**********************************************************
    */

    static class IdWrapper {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
        public ValueNode node;

        public IdWrapper() {}

        public IdWrapper(int v) {
            node = new ValueNode(v);
        }
    }

    static class ValueNode {
        public int value;
        public IdWrapper next;

        public ValueNode() {this(0);}

        public ValueNode(int v) {value = v;}
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testDefaultSetting() {
        assertTrue(MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS));
    }

    public void testSuccessResolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':1}}}");

        IdWrapper wrapper = MAPPER.readerFor(IdWrapper.class)
            .with(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
            .readValue(json);

        assertSame(wrapper.node, wrapper.node.next.node);
        assertSame(wrapper.node.next.node, wrapper.node.next.node.next.node);
    }

    public void testUnresolvedObjectIdsFailure() throws Exception {
        // Object id 2 is unresolved
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        try {
            // This will also throw exception, enabled by default
            MAPPER.readerFor(IdWrapper.class)
                .readValue(json);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }

        try {
            // This will also throw exception, same as default
            MAPPER.readerFor(IdWrapper.class)
                .with(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .readValue(json);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }
    }

    public void testUnresolvedObjectIdsDoesNotFail() throws Exception {
        // Object id 2 is unresolved
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        // But does not fail, because configured such
        IdWrapper wrapper = MAPPER.readerFor(IdWrapper.class)
            .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
            .readValue(json);

        assertNull(wrapper.node.next.node);
    }

    public void testMissingObjectIdsShouldNotFailEitherWay() throws Exception {
        // Object id is just "missing", not unresolved
        String json = a2q("{'node':{'@id':1,'value':7,'next':{}}}");

        // Missing id does not fail either enabled,
        IdWrapper withWrapper = MAPPER.readerFor(IdWrapper.class)
            .with(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
            .readValue(json);
        assertNull(withWrapper.node.next.node);

        // or disabled.
        IdWrapper withoutWrapper = MAPPER.readerFor(IdWrapper.class)
            .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
            .readValue(json);
        assertNull(withoutWrapper.node.next.node);
    }
}

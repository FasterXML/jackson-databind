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

    static class NodeWrapper {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
        public ValueNode node;

        public NodeWrapper() {}

        public NodeWrapper(int v) {
            node = new ValueNode(v);
        }
    }

    static class ValueNode {
        public int value;
        public NodeWrapper next;

        public ValueNode() {this(0);}

        public ValueNode(int v) {value = v;}
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
    */

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper DISABLED_MAPPER = newJsonMapper()
        .configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false);

    private final ObjectMapper ENABLED_MAPPER = newJsonMapper()
        .configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, true);

    public void testDefaultSetting() {
        assertTrue(DEFAULT_MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS));
    }

    public void testSuccessResolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':1}}}");

        NodeWrapper wrapper = DEFAULT_MAPPER.readValue(json, NodeWrapper.class);

        assertSame(wrapper.node, wrapper.node.next.node);
        assertSame(wrapper.node.next.node, wrapper.node.next.node.next.node);
    }

    public void testUnresolvedObjectIdsFailure() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");
        try {
            // This will also throw exception, enabled by default
            DEFAULT_MAPPER.readValue(json, NodeWrapper.class);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }

        try {
            // This will also throw exception, same as default
            ENABLED_MAPPER.readValue(json, NodeWrapper.class);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }
    }

    public void testUnresolvedObjectIdsDoesNotFail() throws Exception {
        // Object id 2 is unresolved
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        // But does not fail, because disabled to fail
        NodeWrapper wrapper = DISABLED_MAPPER.readValue(json, NodeWrapper.class);

        assertNull(wrapper.node.next.node);
    }


    public void testUnresolvableIdShouldFail() throws Exception
    {
        TestObjectIdDeserialization.IdWrapper w = ENABLED_MAPPER
            .readValue(a2q("{'node':123}"), TestObjectIdDeserialization.IdWrapper.class);
        assertNotNull(w);
        assertNull(w.node);
    }

}

package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;

public class TestObjectId extends BaseMapTest
{
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Identifiable
    {
        public int value;

        public Identifiable next;
        
        public Identifiable() { this(0); }
        public Identifiable(int v) {
            value = v;
        }
    }

    static class IdWrapper
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
        public ValueNode node;

        public IdWrapper() { }
        public IdWrapper(int v) {
            node = new ValueNode(v);
        }
    }

    static class ValueNode {
        public int value;
        public IdWrapper next;
        
        public ValueNode() { this(0); }
        public ValueNode(int v) { value = v; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.UUIDGenerator.class, property="#")
    static class UUIDNode
    {
        public int value;
        public UUIDNode parent;
        public UUIDNode first;
        public UUIDNode second;

        public UUIDNode() { this(0); }
        public UUIDNode(int v) { value = v; }
    }
    
    /*
    /*****************************************************
    /* Unit tests; simple class annotation
    /*****************************************************
     */

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";
    
    private final ObjectMapper mapper = new ObjectMapper();

    /*
    public void testSimpleSerializationClass() throws Exception
    {
        Identifiable src = new Identifiable(13);
        src.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_CLASS, json);

        // and ensure that state is cleared in-between as well:
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_CLASS, json);
    }
        
    public void testSimpleDeserializationClass() throws Exception
    {
        // then bring back...
        Identifiable result = mapper.readValue(EXP_SIMPLE_INT_CLASS, Identifiable.class);
        assertEquals(13, result.value);
        assertSame(result, result.next);
    }

    public void testSimpleUUIDForClassRoundTrip() throws Exception
    {
        UUIDNode root = new UUIDNode(1);
        UUIDNode child1 = new UUIDNode(2);
        UUIDNode child2 = new UUIDNode(3);
        root.first = child1;
        root.second = child2;
        child1.parent = root;
        child2.parent = root;
        child1.first = child2;

        String json = mapper.writeValueAsString(root);

        // and should come back the same too...
        UUIDNode result = mapper.readValue(json, UUIDNode.class);
        assertEquals(1, result.value);
        UUIDNode result2 = result.first;
        UUIDNode result3 = result.second;
        assertNotNull(result2);
        assertNotNull(result3);
        assertEquals(2, result2.value);
        assertEquals(3, result3.value);

        assertSame(result, result2.parent);
        assertSame(result, result3.parent);
        assertSame(result3, result2.first);
    }
    */
    
    /*
    /*****************************************************
    /* Unit tests; simple property annotation
    /*****************************************************
     */

    // Bit more complex, due to extra wrapping etc:
    private final static String EXP_SIMPLE_INT_PROP = "{\"node\":{\"@id\":1,\"value\":7,\"next\":{\"node\":1}}}";

    /*
    public void testSimpleSerializationProperty() throws Exception
    {
        IdWrapper src = new IdWrapper(7);
        src.node.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_PROP, json);
        // and second time too, for a good measure
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_PROP, json);
    }
    */
        
    public void testSimpleDeserializationProperty() throws Exception
    {
        // then bring back...
        IdWrapper result = mapper.readValue(EXP_SIMPLE_INT_PROP, IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }
}

package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;

/**
 * Unit test to verify handling of Object Id deserialization
 */
public class TestObjectIdSerialization extends BaseMapTest
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

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="customId")
    static class IdentifiableWithProp
    {
        // Property that contains Object Id to use
        public int customId;
        
        public int value;

        public IdentifiableWithProp next;
        
        public IdentifiableWithProp() { this(0, 0); }
        public IdentifiableWithProp(int id, int value) {
            this.customId = id;
            this.value = value;
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
    /* Unit tests
    /*****************************************************
     */

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";
    
    private final ObjectMapper mapper = new ObjectMapper();

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

    private final static String EXP_SIMPLE_PROP = "{\"customId\":123,\"value\":-19,\"next\":123}";

    public void testSimpleFromPropertyOnClass() throws Exception
    {
        IdentifiableWithProp src = new IdentifiableWithProp(123, -19);
        src.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_PROP, json);

        // and ensure that state is cleared in-between as well:
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_PROP, json);
    }
    
    // Bit more complex, due to extra wrapping etc:
    private final static String EXP_SIMPLE_INT_PROP = "{\"node\":{\"@id\":1,\"value\":7,\"next\":{\"node\":1}}}";

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
}

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
        public int value;

        // Property that contains Object Id to use
        public int customId;

        public IdentifiableWithProp next;
        
        public IdentifiableWithProp() { this(0, 0); }
        public IdentifiableWithProp(int id, int value) {
            this.customId = id;
            this.value = value;
        }
    }

    // For property reference, need another class:
    
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

    // Similarly for property-ref via property:
    
    protected static class IdWrapperCustom
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
        public ValueNodeCustom node;

        public IdWrapperCustom() { }
        public IdWrapperCustom(int id, int value) {
            node = new ValueNodeCustom(id, value);
        }
    }

    protected static class ValueNodeCustom {
        public int value;
        private int id;
        public IdWrapperCustom next;

        public int getId() { return id; }
        
        public ValueNodeCustom() { this(0, 0); }
        public ValueNodeCustom(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    // // Let's also have one 'broken' test

    // no "id" property
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    static class Broken
    {
        public int value;
        public int customId;
    }
    
    /*
    /*****************************************************
    /* Unit tests, external id serialization
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

    /*
    /*****************************************************
    /* Unit tests, custom (property) id serialization
    /*****************************************************
     */

    private final static String EXP_CUSTOM_PROP = "{\"customId\":123,\"value\":-19,\"next\":123}";
    // Test for verifying that custom
    public void testCustomPropertyForClass() throws Exception
    {
        IdentifiableWithProp src = new IdentifiableWithProp(123, -19);
        src.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP, json);

        // and ensure that state is cleared in-between as well:
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP, json);
    }

    private final static String EXP_CUSTOM_PROP_VIA_REF = "{\"node\":{\"id\":123,\"value\":7,\"next\":{\"node\":123}}}";
    // Test for verifying that custom
    public void testCustomPropertyViaProperty() throws Exception
    {
        IdWrapperCustom src = new IdWrapperCustom(123, 7);
        src.node.next = src;
        
        // First, serialize:
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP_VIA_REF, json);
        // and second time too, for a good measure
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP_VIA_REF, json);
    }

    /*
    /*****************************************************
    /* Unit tests, error handling
    /*****************************************************
     */

    public void testInvalidProp() throws Exception
    {
        try {
            mapper.writeValueAsString(new Broken());
            fail("Should have thrown an exception");
        } catch (JsonMappingException e) {
            verifyException(e, "can not find property with name 'id'");
        }
    }
}

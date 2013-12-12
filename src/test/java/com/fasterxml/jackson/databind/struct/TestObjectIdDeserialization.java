package com.fasterxml.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.struct.TestObjectId.Company;
import com.fasterxml.jackson.databind.struct.TestObjectId.Employee;

/**
 * Unit test to verify handling of Object Id deserialization
 */
public class TestObjectIdDeserialization extends BaseMapTest
{
    // // Classes for external id use
    
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
    
    // // Classes for external id from property annotations:
    
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

    // // Classes for external id use

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="customId")
    static class IdentifiableCustom
    {
        public int value;

        public int customId;
        
        public IdentifiableCustom next;
        
        public IdentifiableCustom() { this(-1, 0); }
        public IdentifiableCustom(int i, int v) {
            customId = i;
            value = v;
        }
    }

    static class IdWrapperExt
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class,
        		property="customId")
        public ValueNodeExt node;

        public IdWrapperExt() { }
        public IdWrapperExt(int v) {
            node = new ValueNodeExt(v);
        }
    }

    static class ValueNodeExt
    {
        public int value;
        private int customId;
        public IdWrapperExt next;
        
        public ValueNodeExt() { this(0); }
        public ValueNodeExt(int v) { value = v; }

        public void setCustomId(int i) {
        	customId = i;
        }
    }
    
    static class MappedCompany {
        public Map<Integer, Employee> employees;
    }

    static class ArrayCompany {
        public Employee[] employees;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class AnySetterObjectId {
        private Map<String, AnySetterObjectId> values = new HashMap<String, AnySetterObjectId>();

        @JsonAnySetter
        public void anySet(String field, AnySetterObjectId value) {
            // Ensure that it is never called with null because of unresolved reference.
            assertNotNull(value);
            values.put(field, value);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();
    
    /*
    /*****************************************************
    /* Unit tests, external id deserialization
    /*****************************************************
     */

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";

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

    // Bit more complex, due to extra wrapping etc:
    private final static String EXP_SIMPLE_INT_PROP = "{\"node\":{\"@id\":1,\"value\":7,\"next\":{\"node\":1}}}";
        
    public void testSimpleDeserializationProperty() throws Exception
    {
        IdWrapper result = mapper.readValue(EXP_SIMPLE_INT_PROP, IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    // Another test to ensure ordering is not required (i.e. can do front references)
    public void testSimpleDeserWithForwardRefs() throws Exception
    {
        IdWrapper result = mapper.readValue("{\"node\":{\"value\":7,\"next\":{\"node\":1}, \"@id\":1}}"
                ,IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    public void testLateForwardReferenceInCollection() throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "]}";
        Company company = mapper.readValue(json, Company.class);
        assertEquals(2, company.employees.size());
        // Deser must keep object ordering.
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
        assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
    }

    // Variant of before but forward reference is not "wrapped" inside a collection, might be easier to fix first.
    public void testLateForwardReference() throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":2,\"reports\":[]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":null,\"reports\":[1]}"
                      + "]}";
        Company company = mapper.readValue(json, Company.class);
        assertEquals(2, company.employees.size());
        // Deser must keep object ordering.
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(secondEmployee, firstEmployee.manager); // Ensure that forward reference was properly resolved.
        assertEquals(firstEmployee, secondEmployee.reports.get(0)); // And that back reference is also properly resolved.
    }

    public void testLateForwardReferenceInMap() throws Exception
    {
        String json = "{\"employees\":{"
                      + "\"1\":{\"id\":1,\"name\":\"First\",\"manager\":2,\"reports\":[]},"
                      + "\"2\": 2,"
                      + "\"3\":{\"id\":2,\"name\":\"Second\",\"manager\":null,\"reports\":[1]}"
                      + "}}";
        MappedCompany company = mapper.readValue(json, MappedCompany.class);
        assertEquals(3, company.employees.size());
        // Deser must keep object ordering.
        Employee firstEmployee = company.employees.get(1);
        Employee secondEmployee = company.employees.get(3);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(secondEmployee, firstEmployee.manager); // Ensure that forward reference was properly resolved.
        assertEquals(firstEmployee, secondEmployee.reports.get(0)); // And that back reference is also properly resolved.
    }

    public void testLateForwardReferenceInArray() throws Exception {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "2,{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "]}";
        ArrayCompany company = mapper.readValue(json, ArrayCompany.class);
        assertEquals(3, company.employees.length);
        // Deser must keep object ordering.
        Employee firstEmployee = company.employees[0];
        Employee secondEmployee = company.employees[1];
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
        assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
    }

    public void testForwardReferenceAnySetterCombo() throws Exception {
        String json = "{\"@id\":1, \"foo\":2, \"bar\":{\"@id\":2, \"foo\":1}}";
        AnySetterObjectId value = mapper.readValue(json, AnySetterObjectId.class);
        assertSame(value.values.get("bar"), value.values.get("foo"));
    }

    /*
    /*****************************************************
    /* Unit tests, custom (property-based) id deserialization
    /*****************************************************
     */

    private final static String EXP_CUSTOM_VIA_CLASS = "{\"customId\":123,\"value\":-900,\"next\":123}";

    public void testCustomDeserializationClass() throws Exception
    {
        // then bring back...
        IdentifiableCustom result = mapper.readValue(EXP_CUSTOM_VIA_CLASS, IdentifiableCustom.class);
        assertEquals(-900, result.value);
        assertSame(result, result.next);
    }

    private final static String EXP_CUSTOM_VIA_PROP = "{\"node\":{\"customId\":3,\"value\":99,\"next\":{\"node\":3}}}";
    
    public void testCustomDeserializationProperty() throws Exception
    {
        // then bring back...
    	IdWrapperExt result = mapper.readValue(EXP_CUSTOM_VIA_PROP, IdWrapperExt.class);
        assertEquals(99, result.node.value);
        assertSame(result.node, result.node.next.node);
        assertEquals(3, result.node.customId);
    }
}

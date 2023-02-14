package com.fasterxml.jackson.databind.objectid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;
import com.fasterxml.jackson.databind.deser.UnresolvedId;
import com.fasterxml.jackson.databind.objectid.TestObjectId.Company;
import com.fasterxml.jackson.databind.objectid.TestObjectId.Employee;

/**
 * Unit test to verify handling of Object Id deserialization
 */
public class TestObjectIdDeserialization extends BaseMapTest
{
    private static final String POOL_KEY = "POOL";

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
        protected int customId;
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

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class AnySetterObjectId {
        protected Map<String, AnySetterObjectId> values = new HashMap<String, AnySetterObjectId>();

        @JsonAnySetter
        public void anySet(String field, AnySetterObjectId value) {
            // Ensure that it is never called with null because of unresolved reference.
            assertNotNull(value);
            values.put(field, value);
        }
    }

    static class CustomResolutionWrapper {
        public List<WithCustomResolution> data;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = PoolResolver.class)
    @JsonIdentityReference(alwaysAsId = true) // #524
    static class WithCustomResolution {
        public int id;
        public int data;

        public WithCustomResolution(int id, int data)
        {
            this.id = id;
            this.data = data;
        }
    }

    public static class PoolResolver implements ObjectIdResolver {
        private Map<Object,WithCustomResolution> _pool;

        public PoolResolver() {}
        public PoolResolver(Map<Object,WithCustomResolution> pool){ _pool = pool; }

        @Override
        public void bindItem(IdKey id, Object pojo){ }

        @Override
        public Object resolveId(IdKey id){ return _pool.get(id.key); }

        @Override
        public boolean canUseFor(ObjectIdResolver resolverType)
        {
            return resolverType.getClass() == getClass() && _pool != null && !_pool.isEmpty();
        }

        @Override
        public ObjectIdResolver newForDeserialization(Object c)
        {
            DeserializationContext context = (DeserializationContext)c;
            @SuppressWarnings("unchecked")
            Map<Object,WithCustomResolution> pool = (Map<Object,WithCustomResolution>)context.getAttribute(POOL_KEY);
            return new PoolResolver(pool);
        }
    }

    /*
    /*****************************************************
    /* Unit tests, external id deserialization
    /*****************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";

    public void testSimpleDeserializationClass() throws Exception
    {
        // then bring back...
        Identifiable result = MAPPER.readValue(EXP_SIMPLE_INT_CLASS, Identifiable.class);
        assertEquals(13, result.value);
        assertSame(result, result.next);
    }

    // Should be ok NOT to have Object id, as well
    public void testMissingObjectId() throws Exception
    {
        Identifiable result = MAPPER.readValue(a2q("{'value':28, 'next':{'value':29}}"),
                Identifiable.class);
        assertNotNull(result);
        assertEquals(28, result.value);
        assertNotNull(result.next);
        assertEquals(29, result.next.value);
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

        String json = MAPPER.writeValueAsString(root);

        // and should come back the same too...
        UUIDNode result = MAPPER.readValue(json, UUIDNode.class);
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
        IdWrapper result = MAPPER.readValue(EXP_SIMPLE_INT_PROP, IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    // Another test to ensure ordering is not required (i.e. can do front references)
    public void testSimpleDeserWithForwardRefs() throws Exception
    {
        IdWrapper result = MAPPER.readValue("{\"node\":{\"value\":7,\"next\":{\"node\":1}, \"@id\":1}}"
                ,IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    public void testForwardReference()
        throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":2,\"reports\":[]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":null,\"reports\":[1]}"
                      + "]}";
        Company company = MAPPER.readValue(json, Company.class);
        assertEquals(2, company.employees.size());
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(secondEmployee, firstEmployee.manager); // Ensure that forward reference was properly resolved.
        assertEquals(firstEmployee, secondEmployee.reports.get(0)); // And that back reference is also properly resolved.
    }

    public void testForwardReferenceInCollection()
        throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "]}";
        Company company = MAPPER.readValue(json, Company.class);
        assertEquals(2, company.employees.size());
        Employee firstEmployee = company.employees.get(0);
        Employee secondEmployee = company.employees.get(1);
        assertEmployees(firstEmployee, secondEmployee);
    }

    public void testForwardReferenceInMap()
        throws Exception
    {
        String json = "{\"employees\":{"
                      + "\"1\":{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "\"2\": 2,"
                      + "\"3\":{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "}}";
        MappedCompany company = MAPPER.readValue(json, MappedCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.get(1);
        Employee secondEmployee = company.employees.get(3);
        assertEmployees(firstEmployee, secondEmployee);
    }

    private void assertEmployees(Employee firstEmployee, Employee secondEmployee)
    {
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0)); // Ensure that forward reference was properly resolved and in order.
        assertSame(firstEmployee, secondEmployee.manager); // And that back reference is also properly resolved.
    }

    public void testForwardReferenceAnySetterCombo() throws Exception {
        String json = "{\"@id\":1, \"foo\":2, \"bar\":{\"@id\":2, \"foo\":1}}";
        AnySetterObjectId value = MAPPER.readValue(json, AnySetterObjectId.class);
        assertSame(value.values.get("bar"), value.values.get("foo"));
    }

    public void testUnresolvedForwardReference()
        throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[3]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":3,\"reports\":[]}"
                      + "]}";
        try {
            MAPPER.readValue(json, Company.class);
            fail("Should have thrown.");
        } catch (UnresolvedForwardReference exception) {
            // Expected
            List<UnresolvedId> unresolvedIds = exception.getUnresolvedIds();
            assertEquals(2, unresolvedIds.size());
            UnresolvedId firstUnresolvedId = unresolvedIds.get(0);
            assertEquals(3, firstUnresolvedId.getId());
            assertEquals(Employee.class, firstUnresolvedId.getType());
            UnresolvedId secondUnresolvedId = unresolvedIds.get(1);
            assertEquals(firstUnresolvedId.getId(), secondUnresolvedId.getId());
            assertEquals(Employee.class, secondUnresolvedId.getType());
        }
    }

    // [databind#299]: Allow unresolved ids to become nulls
    public void testUnresolvableAsNull() throws Exception
    {
        IdWrapper w = MAPPER.readerFor(IdWrapper.class)
                .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .readValue(a2q("{'node':123}"));
        assertNotNull(w);
        assertNull(w.node);
    }

    public void testKeepCollectionOrdering() throws Exception
    {
        String json = "{\"employees\":[2,1,"
                + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "]}";
        Company company = MAPPER.readValue(json, Company.class);
        assertEquals(4, company.employees.size());
        // Deser must keep object ordering.
        Employee firstEmployee = company.employees.get(1);
        Employee secondEmployee = company.employees.get(0);
        assertSame(firstEmployee, company.employees.get(2));
        assertSame(secondEmployee, company.employees.get(3));
        assertEmployees(firstEmployee, secondEmployee);
    }

    public void testKeepMapOrdering()
        throws Exception
    {
        String json = "{\"employees\":{"
                      + "\"1\":2, \"2\":1,"
                      + "\"3\":{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                      + "\"4\":{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                      + "}}";
        MappedCompany company = MAPPER.readValue(json, MappedCompany.class);
        assertEquals(4, company.employees.size());
        Employee firstEmployee = company.employees.get(2);
        Employee secondEmployee = company.employees.get(1);
        assertEmployees(firstEmployee, secondEmployee);
        // Deser must keep object ordering. Not sure if it's really important for maps,
        // but since default map is LinkedHashMap might as well ensure it does...
        Iterator<Entry<Integer,Employee>> iterator = company.employees.entrySet().iterator();
        assertSame(secondEmployee, iterator.next().getValue());
        assertSame(firstEmployee, iterator.next().getValue());
        assertSame(firstEmployee, iterator.next().getValue());
        assertSame(secondEmployee, iterator.next().getValue());
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
        IdentifiableCustom result = MAPPER.readValue(EXP_CUSTOM_VIA_CLASS, IdentifiableCustom.class);
        assertEquals(-900, result.value);
        assertSame(result, result.next);
    }

    private final static String EXP_CUSTOM_VIA_PROP = "{\"node\":{\"customId\":3,\"value\":99,\"next\":{\"node\":3}}}";

    public void testCustomDeserializationProperty() throws Exception
    {
        // then bring back...
        IdWrapperExt result = MAPPER.readValue(EXP_CUSTOM_VIA_PROP, IdWrapperExt.class);
        assertEquals(99, result.node.value);
        assertSame(result.node, result.node.next.node);
        assertEquals(3, result.node.customId);
    }

    /*
    /*****************************************************
    /* Unit tests, custom id resolver
    /*****************************************************
     */

    public void testCustomPoolResolver() throws Exception
    {
        Map<Object,WithCustomResolution> pool = new HashMap<Object,WithCustomResolution>();
        pool.put(1, new WithCustomResolution(1, 1));
        pool.put(2, new WithCustomResolution(2, 2));
        pool.put(3, new WithCustomResolution(3, 3));
        pool.put(4, new WithCustomResolution(4, 4));
        pool.put(5, new WithCustomResolution(5, 5));
        ContextAttributes attrs = MAPPER.getDeserializationConfig().getAttributes().withSharedAttribute(POOL_KEY, pool);
        String content = "{\"data\":[1,2,3,4,5]}";
        CustomResolutionWrapper wrapper = MAPPER.readerFor(CustomResolutionWrapper.class).with(attrs).readValue(content);
        assertFalse(wrapper.data.isEmpty());
        for (WithCustomResolution ob : wrapper.data) {
            assertSame(pool.get(ob.id), ob);
        }
    }

    /*
    /*****************************************************
    /* Unit tests, missing/null Object id [databind#742]
    /*****************************************************
     */

    /*
    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Identifiable
    {
        public int value;

        public Identifiable next;
    }
    */

    public void testNullObjectId() throws Exception
    {
        // Ok, so missing Object Id is ok, but so is null.

        Identifiable value = MAPPER.readValue
                (a2q("{'value':3, 'next':null, 'id':null}"), Identifiable.class);
        assertNotNull(value);
        assertEquals(3, value.value);
    }
}

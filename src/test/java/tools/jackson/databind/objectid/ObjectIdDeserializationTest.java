package tools.jackson.databind.objectid;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ContextAttributes;
import tools.jackson.databind.deser.UnresolvedForwardReference;
import tools.jackson.databind.deser.UnresolvedId;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify handling of Object Id deserialization.
 */
public class ObjectIdDeserializationTest extends DatabindTestUtil
{
    private static final String POOL_KEY = "POOL";

    // // // Classes for external id use

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Identifiable
    {
        public int value;
        public Identifiable next;

        public Identifiable() { this(0); }
        public Identifiable(int v) { value = v; }
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

    // // // Classes for external id from property annotations:

    static class IdWrapper
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
        public ValueNode node;

        public IdWrapper() { }
        public IdWrapper(int v) { node = new ValueNode(v); }
    }

    static class ValueNode {
        public int value;
        public IdWrapper next;

        public ValueNode() { this(0); }
        public ValueNode(int v) { value = v; }
    }

    // // // Classes for custom property id use

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="customId")
    static class IdentifiableCustom
    {
        public int value;
        public int customId;
        public IdentifiableCustom next;

        public IdentifiableCustom() { this(-1, 0); }
        protected IdentifiableCustom(int i, int v) {
            customId = i;
            value = v;
        }
    }

    static class IdWrapperExt
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="customId")
        public ValueNodeExt node;

        public IdWrapperExt() { }
        public IdWrapperExt(int v) { node = new ValueNodeExt(v); }
    }

    static class ValueNodeExt
    {
        public int value;
        protected int customId;
        public IdWrapperExt next;

        public ValueNodeExt() { this(0); }
        public ValueNodeExt(int v) { value = v; }

        public void setCustomId(int i) { customId = i; }
    }

    static class MappedCompany {
        public Map<Integer, Employee> employees;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class AnySetterObjectId {
        protected Map<String, AnySetterObjectId> values = new HashMap<>();

        @JsonAnySetter
        public void anySet(String field, AnySetterObjectId value) {
            // Ensure that it is never called with null because of unresolved reference.
            assertNotNull(value);
            values.put(field, value);
        }
    }

    // // // Classes for custom id resolver

    static class CustomResolutionWrapper {
        public List<WithCustomResolution> data;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = PoolResolver.class)
    @JsonIdentityReference(alwaysAsId = true) // #524
    static class WithCustomResolution {
        public int id;
        public int data;

        public WithCustomResolution(int id, int data) {
            this.id = id;
            this.data = data;
        }
    }

    public static class PoolResolver implements ObjectIdResolver {
        private Map<Object,WithCustomResolution> _pool;

        public PoolResolver() {}
        public PoolResolver(Map<Object,WithCustomResolution> pool) { _pool = pool; }

        @Override
        public void bindItem(IdKey id, Object pojo) { }

        @Override
        public Object resolveId(IdKey id) { return _pool.get(id.key); }

        @Override
        public boolean canUseFor(ObjectIdResolver resolverType) {
            return resolverType.getClass() == getClass() && _pool != null && !_pool.isEmpty();
        }

        @Override
        public ObjectIdResolver newForDeserialization(Object c) {
            DeserializationContext context = (DeserializationContext) c;
            @SuppressWarnings("unchecked")
            Map<Object,WithCustomResolution> pool =
                (Map<Object,WithCustomResolution>) context.getAttribute(POOL_KEY);
            return new PoolResolver(pool);
        }
    }

    // // // Classes for FAIL_ON_UNRESOLVED_OBJECT_IDS tests

    static class SomeWrapper {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
        public SomeNode node;

        public SomeWrapper() {}
        public SomeWrapper(int v) { node = new SomeNode(v); }
    }

    static class SomeNode {
        public int value;
        public SomeWrapper next;

        public SomeNode() { this(0); }
        public SomeNode(int v) { value = v; }
    }

    // // // Company/Employee for forward-reference tests

    static class Company {
        public List<Employee> employees;

        public void add(Employee e) {
            if (employees == null) {
                employees = new ArrayList<>();
            }
            employees.add(e);
        }
    }

    // // // For testNullStringPropertyId [databind#1150]

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    static class IdentifiableStringId
    {
        public String id;
        public int value;
        public Identifiable next;

        public IdentifiableStringId() { this(0); }
        public IdentifiableStringId(int v) { value = v; }
    }

    // // // For testAtomicReferenceWithObjectId [from ReferentialWithObjectIdTest]

    public static class LinkedEmployeeList {
        public AtomicReference<LinkedEmployee> first;
    }

    @JsonIdentityInfo(property="id", generator=ObjectIdGenerators.PropertyGenerator.class)
    public static class LinkedEmployee {
        public int id;
        public String name;
        public AtomicReference<LinkedEmployee> next;

        public LinkedEmployee next(LinkedEmployee n) {
            next = new AtomicReference<>(n);
            return this;
        }
    }

    // // // For testObjectIdWithInjectables [from ObjectIdWithInjectables538Test]

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
    public static class InjectableA {
        public InjectableB b;

        public InjectableA(@JacksonInject("i1") String injected) { }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class)
    public static class InjectableB {
        public InjectableA a;

        @JsonCreator
        public InjectableB(@JacksonInject("i2") String injected) { }
    }

    // // // For testForwardReferenceInEnumMap

    static class EnumMapCompany {
        public EnumMap<FooEnum, Employee> employees;
    }

    static enum FooEnum {
        A, B, C
    }

    // // // For testOrdering1388 [databind#1388]

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    public static class NamedThing {
        private final UUID id;
        private final String name;

        @JsonCreator
        public NamedThing(@JsonProperty("id") UUID id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedThing that = (NamedThing) o;
            return that.id.equals(id) && that.name.equals(name);
        }

        @Override
        public int hashCode() { return name.hashCode(); }
    }

    // // // For [databind#2955]: unresolved scalar Object Id with FAIL_ON_UNRESOLVED_OBJECT_IDS disabled

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Node2955 {
        public int id;
        public String name;
        public Node2955 ref;
    }

    // // // For [databind#2955] / jackson-jaxrs-providers#189: mixed forward, invalid,
    //       missing and inline references with FAIL_ON_UNRESOLVED_OBJECT_IDS disabled

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
            property = "name", scope = Value2955.class)
    static class Value2955 {
        public String name;
        public Integer value;
    }

    static class Owned2955 {
        public String name;
        public Value2955 optionalValue;
    }

    static class Owner2955 {
        public List<Owned2955> owned = new ArrayList<>();
        public List<Value2955> values = new ArrayList<>();
    }

    // // // For ObjectReader + FAIL_ON_UNRESOLVED_OBJECT_IDS [databind#5542]

    public static class ReaderWrapper5542 {
        public ReaderValueNode5542 node;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    public static class ReaderValueNode5542 {
        public int value;
        public ReaderWrapper5542 next;
    }

    // // // For testWithFieldsInBaseClass [databind#1083]

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            property = "type",
            defaultImpl = JsonMapSchema.class)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = JsonMapSchema.class, name = "map"),
        @JsonSubTypes.Type(value = JsonJdbcSchema.class, name = "jdbc") })
    public static abstract class JsonSchema {
        public String name;
    }

    static class JsonMapSchema extends JsonSchema { }

    static class JsonJdbcSchema extends JsonSchema { }

    static class JsonRoot1083 {
        public List<JsonSchema> schemas = new ArrayList<>();
    }

    /*
    /**********************************************************
    /* Unit tests, external id deserialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"value\":13,\"next\":1}";

    @Test
    public void testSimpleDeserializationClass() throws Exception
    {
        Identifiable result = MAPPER.readValue(EXP_SIMPLE_INT_CLASS, Identifiable.class);
        assertEquals(13, result.value);
        assertSame(result, result.next);
    }

    // Should be ok NOT to have Object id, as well
    @Test
    public void testMissingObjectId() throws Exception
    {
        Identifiable result = MAPPER.readValue(a2q("{'value':28, 'next':{'value':29}}"),
                Identifiable.class);
        assertNotNull(result);
        assertEquals(28, result.value);
        assertNotNull(result.next);
        assertEquals(29, result.next.value);
    }

    @Test
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

    @Test
    public void testSimpleDeserializationProperty() throws Exception
    {
        IdWrapper result = MAPPER.readValue(EXP_SIMPLE_INT_PROP, IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    // Another test to ensure ordering is not required (i.e. can do front references)
    @Test
    public void testSimpleDeserWithForwardRefs() throws Exception
    {
        IdWrapper result = MAPPER.readValue("{\"node\":{\"value\":7,\"next\":{\"node\":1}, \"@id\":1}}"
                ,IdWrapper.class);
        assertEquals(7, result.node.value);
        assertSame(result.node, result.node.next.node);
    }

    @Test
    public void testForwardReference() throws Exception
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
        assertEquals(secondEmployee, firstEmployee.manager); // Ensure forward reference was properly resolved.
        assertEquals(firstEmployee, secondEmployee.reports.get(0)); // And back reference.
    }

    @Test
    public void testForwardReferenceInCollection() throws Exception
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

    @Test
    public void testForwardReferenceInMap() throws Exception
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

    private void assertEmployees(Employee firstEmployee, Employee secondEmployee) {
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0));
        assertSame(firstEmployee, secondEmployee.manager);
    }

    @Test
    public void testForwardReferenceAnySetterCombo() throws Exception {
        String json = "{\"@id\":1, \"foo\":2, \"bar\":{\"@id\":2, \"foo\":1}}";
        AnySetterObjectId value = MAPPER.readValue(json, AnySetterObjectId.class);
        assertSame(value.values.get("bar"), value.values.get("foo"));
    }

    @Test
    public void testUnresolvedForwardReference() throws Exception
    {
        String json = "{\"employees\":["
                      + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[3]},"
                      + "{\"id\":2,\"name\":\"Second\",\"manager\":3,\"reports\":[]}"
                      + "]}";
        try {
            MAPPER.readValue(json, Company.class);
            fail("Should have thrown.");
        } catch (UnresolvedForwardReference exception) {
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
    @Test
    public void testUnresolvableAsNull() throws Exception
    {
        IdWrapper w = MAPPER.readerFor(IdWrapper.class)
                .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .readValue(a2q("{'node':123}"));
        assertNotNull(w);
        assertNull(w.node);
    }

    @Test
    public void testKeepCollectionOrdering() throws Exception
    {
        String json = "{\"employees\":[2,1,"
                + "{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "]}";
        Company company = MAPPER.readValue(json, Company.class);
        assertEquals(4, company.employees.size());
        Employee firstEmployee = company.employees.get(1);
        Employee secondEmployee = company.employees.get(0);
        assertSame(firstEmployee, company.employees.get(2));
        assertSame(secondEmployee, company.employees.get(3));
        assertEmployees(firstEmployee, secondEmployee);
    }

    @Test
    public void testKeepMapOrdering() throws Exception
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
        // Deser must keep object ordering.
        Iterator<Entry<Integer,Employee>> iterator = company.employees.entrySet().iterator();
        assertSame(secondEmployee, iterator.next().getValue());
        assertSame(firstEmployee, iterator.next().getValue());
        assertSame(firstEmployee, iterator.next().getValue());
        assertSame(secondEmployee, iterator.next().getValue());
    }

    /*
    /**********************************************************
    /* Unit tests, custom (property-based) id deserialization
    /**********************************************************
     */

    private final static String EXP_CUSTOM_VIA_CLASS = "{\"customId\":123,\"value\":-900,\"next\":123}";

    @Test
    public void testCustomDeserializationClass() throws Exception
    {
        IdentifiableCustom result = MAPPER.readValue(EXP_CUSTOM_VIA_CLASS, IdentifiableCustom.class);
        assertEquals(-900, result.value);
        assertSame(result, result.next);
    }

    private final static String EXP_CUSTOM_VIA_PROP = "{\"node\":{\"customId\":3,\"value\":99,\"next\":{\"node\":3}}}";

    @Test
    public void testCustomDeserializationProperty() throws Exception
    {
        IdWrapperExt result = MAPPER.readValue(EXP_CUSTOM_VIA_PROP, IdWrapperExt.class);
        assertEquals(99, result.node.value);
        assertSame(result.node, result.node.next.node);
        assertEquals(3, result.node.customId);
    }

    /*
    /**********************************************************
    /* Unit tests, custom id resolver
    /**********************************************************
     */

    @Test
    public void testCustomPoolResolver() throws Exception
    {
        Map<Object,WithCustomResolution> pool = new HashMap<>();
        pool.put(1, new WithCustomResolution(1, 1));
        pool.put(2, new WithCustomResolution(2, 2));
        pool.put(3, new WithCustomResolution(3, 3));
        pool.put(4, new WithCustomResolution(4, 4));
        pool.put(5, new WithCustomResolution(5, 5));
        ContextAttributes attrs = MAPPER.deserializationConfig().getAttributes()
                .withSharedAttribute(POOL_KEY, pool);
        String content = "{\"data\":[1,2,3,4,5]}";
        CustomResolutionWrapper wrapper = MAPPER.readerFor(CustomResolutionWrapper.class)
                .with(attrs).readValue(content);
        assertFalse(wrapper.data.isEmpty());
        for (WithCustomResolution ob : wrapper.data) {
            assertSame(pool.get(ob.id), ob);
        }
    }

    /*
    /**********************************************************
    /* Unit tests, null/missing Object id [databind#742]
    /**********************************************************
     */

    @Test
    public void testNullObjectId() throws Exception
    {
        Identifiable value = MAPPER.readValue(
                a2q("{'value':3, 'next':null, 'id':null}"), Identifiable.class);
        assertNotNull(value);
        assertEquals(3, value.value);
    }

    // [databind#1150]
    @Test
    public void testNullStringPropertyId() throws Exception
    {
        IdentifiableStringId value = MAPPER.readValue(
                a2q("{'value':3, 'next':null, 'id':null}"), IdentifiableStringId.class);
        assertNotNull(value);
        assertEquals(3, value.value);
    }

    /*
    /**********************************************************
    /* Unit tests, DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS
    /**********************************************************
     */

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper DISABLED_MAPPER = jsonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
        .build();

    private final ObjectMapper ENABLED_MAPPER = jsonMapperBuilder()
        .enable(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
        .build();

    @Test
    public void testDefaultSetting() {
        assertTrue(DEFAULT_MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS));
        assertTrue(ENABLED_MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS));
        assertFalse(DISABLED_MAPPER.isEnabled(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS));
    }

    @Test
    public void testSuccessResolvedObjectIds() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':1}}}");

        SomeWrapper wrapper = DEFAULT_MAPPER.readValue(json, SomeWrapper.class);

        assertSame(wrapper.node, wrapper.node.next.node);
        assertSame(wrapper.node.next.node, wrapper.node.next.node.next.node);
    }

    @Test
    public void testUnresolvedObjectIdsFailure() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        // 1. Does not fail with feature disabled
        SomeWrapper wrapper = DISABLED_MAPPER.readValue(json, SomeWrapper.class);
        assertNull(wrapper.node.next.node);

        try {
            // 2. Will fail by default
            DEFAULT_MAPPER.readValue(json, SomeWrapper.class);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }

        try {
            // 3. Will also fail with feature explicitly enabled
            ENABLED_MAPPER.readValue(json, SomeWrapper.class);
            fail("should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference", "Object id [2]");
        }
    }

    /*
    /**********************************************************
    /* Unit tests, type info with JsonIdentityInfo [databind#1083]
    /**********************************************************
     */

    @Test
    public void testWithFieldsInBaseClass1083() throws Exception {
        final String json = a2q("{'schemas': [{\n"
            + "  'name': 'FoodMart'\n"
            + "}]}\n");
        MAPPER.readValue(json, JsonRoot1083.class);
    }

    /*
    /**********************************************************
    /* Unit tests, AtomicReference with ObjectId
    /**********************************************************
     */

    @Test
    public void testAtomicReferenceWithObjectId() throws Exception
    {
        LinkedEmployee first = new LinkedEmployee();
        first.id = 1;
        first.name = "Alice";

        LinkedEmployee second = new LinkedEmployee();
        second.id = 2;
        second.name = "Bob";

        first.next(second);
        second.next(first);

        LinkedEmployeeList input = new LinkedEmployeeList();
        input.first = new AtomicReference<>(first);

        String json = MAPPER.writeValueAsString(input);

        LinkedEmployeeList result = MAPPER.readValue(json, LinkedEmployeeList.class);
        LinkedEmployee firstB = result.first.get();
        assertNotNull(firstB);
        assertEquals("Alice", firstB.name);
        LinkedEmployee secondB = firstB.next.get();
        assertNotNull(secondB);
        assertEquals("Bob", secondB.name);
        assertNotNull(secondB.next.get());
        assertSame(firstB, secondB.next.get());
    }

    /*
    /**********************************************************
    /* Unit tests, ObjectId with injectable values [databind#538]
    /**********************************************************
     */

    @Test
    public void testObjectIdWithInjectables() throws Exception
    {
        InjectableA a = new InjectableA("a");
        InjectableB b = new InjectableB("b");
        a.b = b;
        b.a = a;

        String json = MAPPER.writeValueAsString(a);

        InjectableValues.Std inject = new InjectableValues.Std();
        inject.addValue("i1", "e1");
        inject.addValue("i2", "e2");
        InjectableA output;
        try {
            output = MAPPER.reader(inject).forType(InjectableA.class).readValue(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize from JSON '"+json+"'", e);
        }
        assertNotNull(output);
        assertNotNull(output.b);
        assertSame(output, output.b.a);
    }

    /*
    /**********************************************************
    /* Unit tests, forward reference in EnumMap
    /**********************************************************
     */

    @Test
    public void testForwardReferenceInEnumMap() throws Exception {
        String json = "{\"employees\":{"
                + "\"A\":{\"id\":1,\"name\":\"First\",\"manager\":null,\"reports\":[2]},"
                + "\"B\": 2,"
                + "\"C\":{\"id\":2,\"name\":\"Second\",\"manager\":1,\"reports\":[]}"
                + "}}";
        EnumMapCompany company = MAPPER.readValue(json, EnumMapCompany.class);
        assertEquals(3, company.employees.size());
        Employee firstEmployee = company.employees.get(FooEnum.A);
        Employee secondEmployee = company.employees.get(FooEnum.B);
        assertEquals(1, firstEmployee.id);
        assertEquals(2, secondEmployee.id);
        assertEquals(1, firstEmployee.reports.size());
        assertSame(secondEmployee, firstEmployee.reports.get(0));
        assertSame(firstEmployee, secondEmployee.manager);
    }

    /*
    /**********************************************************
    /* Unit tests, ObjectId reordering [databind#1388]
    /**********************************************************
     */

    private final TypeReference<List<NamedThing>> NAMED_THING_LIST_TYPE =
            new TypeReference<List<NamedThing>>() { };

    // [databind#1388]
    @Test
    public void testOrdering1388() throws Exception
    {
        final UUID id = UUID.fromString("a59aa02c-fe3c-43f8-9b5a-5fe01878a818");
        final NamedThing thing = new NamedThing(id, "Hello");

        {
            final String json = MAPPER.writeValueAsString(Arrays.asList(thing, thing, thing));
            final List<NamedThing> list = MAPPER.readValue(json, NAMED_THING_LIST_TYPE);
            _assertAllSame(list);
            assertTrue(json.equals("[{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"},1,1]"));
        }

        // now move it around to have forward references
        {
            final String json = "[1,1,{\"@id\":1,\"id\":\"a59aa02c-fe3c-43f8-9b5a-5fe01878a818\",\"name\":\"Hello\"}]";
            final List<NamedThing> forward = MAPPER.readValue(json, NAMED_THING_LIST_TYPE);
            _assertAllSame(forward);
        }

        // next, move @id to between properties
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','@id':1,'name':'Hello'}, 1, 1]");
            final List<NamedThing> forward = MAPPER.readValue(json, NAMED_THING_LIST_TYPE);
            _assertAllSame(forward);
        }

        // and last, move @id to be not the first key in the object
        {
            final String json = a2q("[{'id':'a59aa02c-fe3c-43f8-9b5a-5fe01878a818','name':'Hello','@id':1}, 1, 1]");
            final List<NamedThing> forward = MAPPER.readValue(json, NAMED_THING_LIST_TYPE);
            _assertAllSame(forward);
        }
    }

    @Test
    public void testNullsNoObjectId() throws Exception
    {
        final List<NamedThing> l = MAPPER.readValue("[null]", NAMED_THING_LIST_TYPE);
        assertEquals(1, l.size());
        assertNull(l.get(0));
    }

    @Test
    public void testUnresolvedObjectIdReordering() throws Exception
    {
        try {
            MAPPER.readValue("[123]", NAMED_THING_LIST_TYPE);
            fail("Should not pass");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward references: [{Object id: 123}]");
        }
    }

    private void _assertAllSame(List<?> entries) {
        Object first = entries.get(0);
        for (int i = 0, end = entries.size(); i < end; ++i) {
            if (first != entries.get(i)) {
                fail("Mismatch: entry #"+i+" not same as #0");
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests, ObjectReader + FAIL_ON_UNRESOLVED_OBJECT_IDS [databind#5542]
    /**********************************************************
     */

    @Test
    public void testObjectMapperFailsOnUnresolvedObjectIds5542() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        try {
            MAPPER.readValue(json, ReaderWrapper5542.class);
            fail("Should have thrown UnresolvedForwardReference");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference");
        }
    }

    @Test
    public void testObjectReaderFailsOnUnresolvedObjectIds5542() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        ObjectReader reader = MAPPER.readerFor(ReaderWrapper5542.class);
        try (JsonParser p = MAPPER.createParser(json)) {
            reader.readValue(p);
            fail("Should have thrown UnresolvedForwardReference");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Unresolved forward reference");
        }
    }

    @Test
    public void testObjectReaderWithDisabledFeature5542() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':2}}}");

        ObjectReader reader = MAPPER.readerFor(ReaderWrapper5542.class)
                .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS);

        try (JsonParser p = MAPPER.createParser(json)) {
            ReaderWrapper5542 wrapper = reader.readValue(p);
            assertNotNull(wrapper);
            assertNotNull(wrapper.node);
            assertNull(wrapper.node.next.node);
        }
    }

    @Test
    public void testObjectReaderWithResolvedObjectIds5542() throws Exception {
        String json = a2q("{'node':{'@id':1,'value':7,'next':{'node':1}}}");

        ObjectReader reader = MAPPER.readerFor(ReaderWrapper5542.class);
        try (JsonParser p = MAPPER.createParser(json)) {
            ReaderWrapper5542 wrapper = reader.readValue(p);
            assertNotNull(wrapper);
            assertNotNull(wrapper.node);
            assertSame(wrapper.node, wrapper.node.next.node);
        }
    }

    /*
    /**********************************************************
    /* Unit tests, [databind#2955]: unresolved scalar Object Ids
    /**********************************************************
     */

    // [databind#2955]: default behavior should throw on unresolved scalar Object Id
    @Test
    public void testUnresolvedScalarObjectIdFails2955() throws Exception {
        // Node with id=1 references node with id=999 which doesn't exist
        String json = a2q("{'id':1,'name':'a','ref':{'id':2,'name':'b','ref':999}}");

        try {
            MAPPER.readValue(json, Node2955.class);
            fail("Should have thrown UnresolvedForwardReference");
        } catch (UnresolvedForwardReference e) {
            verifyException(e, "Object id");
        }
    }

    // [databind#2955]: with feature disabled, unresolved scalar Object Id should become null
    @Test
    public void testUnresolvedScalarObjectIdAsNull2955() throws Exception {
        String json = a2q("{'id':1,'name':'a','ref':{'id':2,'name':'b','ref':999}}");

        Node2955 result = DISABLED_MAPPER.readValue(json, Node2955.class);
        assertNotNull(result);
        assertEquals("a", result.name);
        assertNotNull(result.ref);
        assertEquals("b", result.ref.name);
        // unresolved id=999 should become null instead of throwing
        assertNull(result.ref.ref);
    }

    // [databind#2955] / jackson-jaxrs-providers#189: with feature disabled, valid forward
    // references must still resolve (not get prematurely turned into null), while genuinely
    // unresolvable scalar references become null.
    @Test
    public void testForwardAndUnresolvedScalarObjectIds2955() throws Exception {
        String json = a2q("{"
                + "'owned':["
                + "  {'name':'foo','optionalValue':'vFoo'},"
                + "  {'name':'bar','optionalValue':'notAValidRef'},"
                + "  {'name':'baz'},"
                + "  {'name':'qux','optionalValue':{'name':'vQux','value':3}}"
                + "],"
                + "'values':["
                + "  {'name':'vFoo','value':1},"
                + "  {'name':'vBar','value':2}"
                + "]}");

        Owner2955 owner = DISABLED_MAPPER.readValue(json, Owner2955.class);

        assertEquals(4, owner.owned.size());
        // Forward reference "vFoo" appears later in "values"; must resolve, not be null
        assertNotNull(owner.owned.get(0).optionalValue);
        assertEquals(Integer.valueOf(1), owner.owned.get(0).optionalValue.value);
        // Reference that never appears anywhere -> null (feature disabled)
        assertNull(owner.owned.get(1).optionalValue);
        // No reference at all -> null
        assertNull(owner.owned.get(2).optionalValue);
        // Inline definition -> resolved directly
        assertNotNull(owner.owned.get(3).optionalValue);
        assertEquals(Integer.valueOf(3), owner.owned.get(3).optionalValue.value);
    }
}

package tools.jackson.databind.objectid;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify handling of Object Id serialization.
 */
public class ObjectIdSerializationTest extends DatabindTestUtil
{
    // // // Int-sequence generator, class-level

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class Identifiable
    {
        public int value;
        public Identifiable next;

        public Identifiable() { this(0); }
        public Identifiable(int v) { value = v; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.StringIdGenerator.class, property="id")
    static class StringIdentifiable
    {
        public int value;
        public StringIdentifiable next;

        public StringIdentifiable() { this(0); }
        public StringIdentifiable(int v) { value = v; }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="customId")
    static class IdentifiableWithProp
    {
        public int value;
        public int customId;
        public IdentifiableWithProp next;

        public IdentifiableWithProp() { this(0, 0); }
        protected IdentifiableWithProp(int id, int value) {
            this.customId = id;
            this.value = value;
        }
    }

    // // // Int-sequence generator, property-level

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

    // // // Property generator, property-level

    protected static class IdWrapperCustom
    {
        @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
        public ValueNodeCustom node;

        public IdWrapperCustom() { }
        protected IdWrapperCustom(int id, int value) {
            node = new ValueNodeCustom(id, value);
        }
    }

    protected static class ValueNodeCustom {
        public int value;
        private int id;
        public IdWrapperCustom next;

        public int getId() { return id; }

        public ValueNodeCustom() { this(0, 0); }
        protected ValueNodeCustom(int id, int value) {
            this.id = id;
            this.value = value;
        }
    }

    // // // alwaysAsId

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
    static class AlwaysAsId
    {
        public int value;

        public AlwaysAsId() { this(0); }
        public AlwaysAsId(int v) { value = v; }
    }

    // For [https://github.com/FasterXML/jackson-annotations/issues/4]
    @JsonPropertyOrder(alphabetic=true)
    static class AlwaysContainer
    {
        @JsonIdentityReference(alwaysAsId=true)
        public AlwaysAsId a = new AlwaysAsId(13);

        @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="id")
        @JsonIdentityReference(alwaysAsId=true)
        public Value b = new Value();
    }

    static class Value {
        public int x = 3;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    static class TreeNode
    {
        public int id;
        public String name;

        @JsonIdentityReference(alwaysAsId=true)
        public TreeNode parent;

        public TreeNode child;

        public TreeNode() { }
        protected TreeNode(TreeNode p, int id, String name) {
            parent = p;
            this.id = id;
            this.name = name;
        }
    }

    // // // [databind#3169]: @JsonIncludeProperties + @JsonIdentityInfo

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "deviceId")
    static class Device3169 {
        public java.util.UUID deviceId;
        public String name;
        public String category;
    }

    static class Config3169 {
        @JsonIncludeProperties({"name"})
        public Device3169 device;
        public Device3169 deviceAgain;
    }

    // // // Error case

    // no "id" property
    @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
    static class Broken
    {
        public int value;
        public int customId;
    }

    // [databind#370]
    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    public static class EmptyObject { }

    // // // For testColumnMetadata

    @JsonPropertyOrder({"a", "b"})
    static class Wrapper {
        public ColumnMetadata a, b;
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    static class ColumnMetadata {
        private final String name;
        private final String type;
        private final String comment;

        @JsonCreator
        public ColumnMetadata(@JsonProperty("name") String name,
                @JsonProperty("type") String type,
                @JsonProperty("comment") String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }

        @JsonProperty("name") public String getName() { return name; }
        @JsonProperty("type") public String getType() { return type; }
        @JsonProperty("comment") public String getComment() { return comment; }
    }

    // // // For testNoDuplicateKeysWithFieldLevelAnnotation [databind#2759]

    static class Hive {
        public String name;
        public List<Bee> bees = new ArrayList<>();
        public Long id;

        Hive() { }

        public Hive(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public void addBee(Bee bee) { bees.add(bee); }
    }

    static class Bee {
        public Long id;

        @JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
        @JsonIdentityReference(alwaysAsId=true)
        @JsonProperty("hiveId")
        Hive hive;

        public Bee() { }

        public Bee(Long id, Hive hive) {
            this.id = id;
            this.hive = hive;
        }

        public Hive getHive() { return hive; }
        public void setHive(Hive hive) { this.hive = hive; }
    }

    // // // For testMixedRefsIssue188 [databind#188]

    static class Company {
        public List<Employee> employees;

        public void add(Employee e) {
            if (employees == null) {
                employees = new ArrayList<>();
            }
            employees.add(e);
        }
    }

    /*
    /**********************************************************
    /* Unit tests, external id serialization
    /**********************************************************
     */

    private final static String EXP_SIMPLE_INT_CLASS = "{\"id\":1,\"next\":1,\"value\":13}";

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper SORTED_MAPPER = jsonMapperBuilder()
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .build();

    @Test
    public void testSimpleSerializationClass() throws Exception
    {
        Identifiable src = new Identifiable(13);
        src.next = src;

        ObjectMapper mapper = SORTED_MAPPER;
        String json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_CLASS, json);

        // and ensure that state is cleared in-between as well:
        json = mapper.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_CLASS, json);
    }

    // Bit more complex, due to extra wrapping etc:
    private final static String EXP_SIMPLE_INT_PROP = "{\"node\":{\"@id\":1,\"next\":{\"node\":1},\"value\":7}}";

    @Test
    public void testSimpleSerializationProperty() throws Exception
    {
        IdWrapper src = new IdWrapper(7);
        src.node.next = src;

        String json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_PROP, json);
        // and second time too, for a good measure
        json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_SIMPLE_INT_PROP, json);
    }

    // [databind#370]
    @Test
    public void testEmptyObjectWithId() throws Exception
    {
        String json = MAPPER.writeValueAsString(new EmptyObject());
        assertEquals(a2q("{'@id':1}"), json);
    }

    @Test
    public void testSerializeWithOpaqueStringId() throws Exception
    {
        StringIdentifiable ob1 = new StringIdentifiable(12);
        StringIdentifiable ob2 = new StringIdentifiable(34);
        ob1.next = ob2;
        ob2.next = ob1;

        // first just verify we get some output
        String json = MAPPER.writeValueAsString(ob1);
        assertNotNull(json);

        // then verify round-trip
        StringIdentifiable output = MAPPER.readValue(json, StringIdentifiable.class);
        assertNotNull(output);
        assertEquals(12, output.value);
        assertNotNull(output.next);
        assertEquals(34, output.next.value);
        assertSame(output.next.next, output);

        String json2 = a2q("{'id':'foobar','value':3, 'next':{'id':'barf','value':5,'next':'foobar'}}");
        output = MAPPER.readValue(json2, StringIdentifiable.class);
        assertNotNull(output);
        assertEquals(3, output.value);
        assertNotNull(output.next);
        assertEquals(5, output.next.value);
        assertSame(output.next.next, output);
    }

    /*
    /**********************************************************
    /* Unit tests, custom (property) id serialization
    /**********************************************************
     */

    private final static String EXP_CUSTOM_PROP = "{\"customId\":123,\"next\":123,\"value\":-19}";

    @Test
    public void testCustomPropertyForClass() throws Exception
    {
        IdentifiableWithProp src = new IdentifiableWithProp(123, -19);
        src.next = src;

        String json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP, json);

        // and ensure that state is cleared in-between as well:
        json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP, json);
    }

    private final static String EXP_CUSTOM_PROP_VIA_REF = "{\"node\":{\"id\":123,\"next\":{\"node\":123},\"value\":7}}";

    @Test
    public void testCustomPropertyViaProperty() throws Exception
    {
        IdWrapperCustom src = new IdWrapperCustom(123, 7);
        src.node.next = src;

        String json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP_VIA_REF, json);
        // and second time too, for a good measure
        json = SORTED_MAPPER.writeValueAsString(src);
        assertEquals(EXP_CUSTOM_PROP_VIA_REF, json);
    }

    @Test
    public void testAlwaysAsId() throws Exception
    {
        String json = MAPPER.writeValueAsString(new AlwaysContainer());
        assertEquals("{\"a\":1,\"b\":2}", json);
    }

    @Test
    public void testAlwaysIdForTree() throws Exception
    {
        TreeNode root = new TreeNode(null, 1, "root");
        TreeNode leaf = new TreeNode(root, 2, "leaf");
        root.child = leaf;
        String json = SORTED_MAPPER.writeValueAsString(root);
        assertEquals("{\"id\":1,\"child\":"
                +"{\"id\":2,\"child\":null,\"name\":\"leaf\",\"parent\":1},\"name\":\"root\",\"parent\":null}",
                json);
    }

    /*
    /**********************************************************
    /* Unit tests, ColumnMetadata roundtrip with @JsonCreator
    /**********************************************************
     */

    @Test
    public void testColumnMetadata() throws Exception
    {
        ColumnMetadata col = new ColumnMetadata("Billy", "employee", "comment");
        Wrapper w = new Wrapper();
        w.a = col;
        w.b = col;
        String json = MAPPER.writeValueAsString(w);

        Wrapper deserialized = MAPPER.readValue(json, Wrapper.class);
        assertNotNull(deserialized);
        assertNotNull(deserialized.a);
        assertNotNull(deserialized.b);

        assertEquals("Billy", deserialized.a.getName());
        assertEquals("employee", deserialized.a.getType());
        assertEquals("comment", deserialized.a.getComment());

        assertSame(deserialized.a, deserialized.b);
    }

    /*
    /**********************************************************
    /* Unit tests, mixed refs (alwaysAsId) serialization [databind#188]
    /**********************************************************
     */

    @Test
    public void testMixedRefsIssue188() throws Exception
    {
        Company comp = new Company();
        Employee e1 = new Employee(1, "First", null);
        Employee e2 = new Employee(2, "Second", e1);
        e1.addReport(e2);
        comp.add(e1);
        comp.add(e2);

        String json = SORTED_MAPPER.writeValueAsString(comp);

        assertEquals("{\"employees\":["
                +"{\"id\":1,\"manager\":null,\"name\":\"First\",\"reports\":[2]},"
                +"{\"id\":2,\"manager\":1,\"name\":\"Second\",\"reports\":[]}"
                +"]}",
                json);
    }

    /*
    /**********************************************************
    /* Unit tests, no duplicate keys with field-level @JsonIdentityInfo [databind#2759]
    /**********************************************************
     */

    // [databind#2759]
    @Test
    public void testNoDuplicateKeysWithFieldLevelAnnotation() throws Exception
    {
        Hive hive = new Hive(100500L, "main hive");
        hive.addBee(new Bee(1L, hive));

        final String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(hive);
        try {
            MAPPER.readerFor(JsonNode.class)
                .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .readValue(json);
        } catch (DatabindException e) {
            fail("Should not have duplicates, but JSON content has: "+json);
        }
    }

    /*
    /**********************************************************
    /* Unit tests, @JsonIncludeProperties + @JsonIdentityInfo [databind#3169]
    /**********************************************************
     */

    // [databind#3169]: @JsonIncludeProperties at reference site should narrow
    // properties of the identity-info'd target, not collapse it to just the id.
    @Test
    public void testIncludePropertiesWithIdentityInfo3169() throws Exception
    {
        Device3169 d = new Device3169();
        d.deviceId = java.util.UUID.fromString("b16c3254-ee2e-11e7-8c3f-fa085a82f01f");
        d.name = "Thermostat";
        d.category = "HVAC";
        Config3169 c = new Config3169();
        c.device = d;
        // second reference verifies identity-info is actually engaged
        c.deviceAgain = d;

        String json = MAPPER.writeValueAsString(c);
        // First occurrence should honor @JsonIncludeProperties({"name"}) — only
        // "name", and importantly NOT collapsed to just the deviceId string.
        assertTrue(json.contains("\"device\":{\"name\":\"Thermostat\"}"),
                "Expected narrowed first occurrence, got: " + json);
    }

    /*
    /**********************************************************
    /* Unit tests, error handling
    /**********************************************************
     */

    @Test
    public void testInvalidProp() throws Exception
    {
        try {
            MAPPER.writeValueAsString(new Broken());
            fail("Should have thrown an exception");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "cannot find property with name 'id'");
        }
    }
}

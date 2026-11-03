package tools.jackson.databind.struct;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for parent-child references (@JsonManagedReference / @JsonBackReference)
public class ParentChildReferencesTest
    extends DatabindTestUtil
{
    /*
    /**********************************************************
    /* Test classes, simple tree (TestParentChildReferences)
    /**********************************************************
     */

    /**
     * First, a simple 'tree': just parent/child linkage
     */
    static class SimpleTreeNode
    {
        public String name;

        // Reference back to parent; reference, ignored during ser,
        // re-constructed during deser
        @JsonBackReference
        public SimpleTreeNode parent;

        // Reference that is serialized normally during ser, back
        // reference within pointed-to instance assigned to point to
        // referring bean ("this")
        @JsonManagedReference
        public SimpleTreeNode child;

        public SimpleTreeNode() { this(null); }
        public SimpleTreeNode(String n) { name = n; }
    }

    static class SimpleTreeNode2
    {
        public String name;
        protected SimpleTreeNode2 parent;
        protected SimpleTreeNode2 child;

        public SimpleTreeNode2() { this(null); }
        public SimpleTreeNode2(String n) { name = n; }

        @JsonBackReference
        public SimpleTreeNode2 getParent() { return parent; }
        public void setParent(SimpleTreeNode2 p) { parent = p; }

        @JsonManagedReference
        public SimpleTreeNode2 getChild() { return child; }
        public void setChild(SimpleTreeNode2 c) { child = c; }
    }

    /**
     * Then nodes with two separate linkages; parent/child
     * and prev/next-sibling
     */
    static class FullTreeNode
    {
        public String name;

        // parent-child links
        @JsonBackReference("parent")
        public FullTreeNode parent;
        @JsonManagedReference("parent")
        public FullTreeNode firstChild;

        // sibling-links
        @JsonManagedReference("sibling")
        public FullTreeNode next;
        @JsonBackReference("sibling")
        protected FullTreeNode prev;

        public FullTreeNode() { this(null); }
        public FullTreeNode(String name) {
            this.name = name;
        }
    }

    /**
     * Class for testing managed references via arrays
     */
    static class NodeArray
    {
        @JsonManagedReference("arr")
        public ArrayNode[] nodes;
    }

    static class ArrayNode
    {
        public String name;

        @JsonBackReference("arr")
        public NodeArray parent;

        public ArrayNode() { this(null); }
        public ArrayNode(String n) { name = n; }
    }

    /**
     * Class for testing managed references via Collections
     */
    static class NodeList
    {
        @JsonManagedReference
        public List<NodeForList> nodes;
    }

    static class NodeForList
    {
        public String name;

        @JsonBackReference
        public NodeList parent;

        public NodeForList() { this(null); }
        public NodeForList(String n) { name = n; }
    }

    static class NodeMap
    {
        @JsonManagedReference
        public Map<String,NodeForMap> nodes;
    }

    static class NodeForMap
    {
        public String name;

        @JsonBackReference
        public NodeMap parent;

        public NodeForMap() { this(null); }
        public NodeForMap(String n) { name = n; }
    }

    public static class Parent {
        @JsonManagedReference
        protected final List<Child> children = new ArrayList<Child>();

        public List<Child> getChildren() { return children; }

        public void addChild(Child child) { children.add(child); child.setParent(this); }
    }

    public static class Child {
        protected Parent parent;
        protected final String value; // So that the bean is not empty of properties

        public Child(@JsonProperty("value") String value) { this.value = value; }

        public String getValue() { return value; }

        @JsonBackReference
        public Parent getParent() { return parent; }

        public void setParent(Parent parent) { this.parent = parent; }
    }

    @JsonTypeInfo(use=Id.NAME)
    @JsonSubTypes({@JsonSubTypes.Type(ConcreteNode.class)})
    static abstract class AbstractNode
    {
        public String id;

        @JsonManagedReference public AbstractNode next;
        @JsonBackReference public AbstractNode prev;
    }

    @JsonTypeName("concrete")
    static class ConcreteNode extends AbstractNode {
        public ConcreteNode() { }
        public ConcreteNode(String id) { this.id = id; }
    }

    // [databind#1878]
    static class Child1878 {
        @JsonBackReference
        public Parent1878 b;
    }

    static class Parent1878 {
        @JsonManagedReference
        public Child1878 a;
    }

    // Forward reference with @JsonIdentityInfo
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY)
    static class ForwardReferenceContainerClass
    {
        public ForwardReferenceClass frc;
        public YetAnotherClass2 yac;
        public String id;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ForwardReferenceClassOne.class, name = "One"),
            @JsonSubTypes.Type(value = ForwardReferenceClassTwo.class, name = "Two")})
    static abstract class ForwardReferenceClass
    {
        public String id;
        public void setId(String id) {
            this.id = id;
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    static class YetAnotherClass2
    {
        public YetAnotherClass2() {}
        public ForwardReferenceClass frc;
        public String id;
    }

    static class ForwardReferenceClassOne extends ForwardReferenceClass { }

    static class ForwardReferenceClassTwo extends ForwardReferenceClass { }

    // [JACKSON-708]
    static class Model708 { }

    static class Advertisement708 extends Model708 {
        public String title;
        @JsonManagedReference public List<Photo708> photos;
    }

    static class Photo708 extends Model708 {
        public int id;
        @JsonBackReference public Advertisement708 advertisement;
    }

    /*
    /**********************************************************
    /* Test classes, misc back-reference edge cases (BackReferenceMiscTest)
    /**********************************************************
     */

    // [JACKSON-890]

    @JsonPropertyOrder(alphabetic=true)
    interface Entity
    {
        @JsonIgnore String getEntityType();
        Long getId();
        void setId(Long id);
        @JsonIgnore void setPersistable();
    }

    @JsonDeserialize(as = NestedPropertySheetImpl.class)
    interface NestedPropertySheet
        extends Property<PropertySheet>
    {
        @Override PropertySheet getValue();
        void setValue(PropertySheet propertySheet);
    }

    @JsonDeserialize(as = AbstractProperty.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include  = JsonTypeInfo.As.PROPERTY,
        property = "@class")
    interface Property<T> extends Entity
    {
        String getName();
        PropertySheet getParentSheet();
        T getValue();
        void setName(String name);
        void setParentSheet(PropertySheet parentSheet);
    }

    @JsonDeserialize(as = PropertySheetImpl.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include  = JsonTypeInfo.As.PROPERTY,
        property = "@class")
    @SuppressWarnings("rawtypes")
    interface PropertySheet extends Entity
    {
        void addProperty(Property property);
        Map<String, Property> getProperties();
        void setProperties(Map<String, Property> properties);
    }

    @JsonDeserialize(as = StringPropertyImpl.class)
    interface StringProperty
        extends Property<String>
    {
        @Override String getValue();
        void setValue(String value);
    }

    static class AbstractEntity implements Entity
    {
        private long id;

        @Override public String getEntityType() {
            return "";
        }

        @Override public Long getId() {
            return id;
        }

        @Override public void setId(Long id)
        {
            this.id = id;
        }

        @Override public void setPersistable() { }
    }

    abstract static class AbstractProperty<T>
        extends AbstractEntity
        implements Property<T>
    {
        private String        m_name;
        private PropertySheet m_parentSheet;

        protected AbstractProperty() { }

        protected AbstractProperty(String name) {
            m_name = name;
        }

        @Override public String getName() {
            return m_name;
        }

        @JsonBackReference("propertySheet-properties")
        @Override public PropertySheet getParentSheet() {
            return m_parentSheet;
        }

        @Override public void setName(String name) {
            m_name = name;
        }

        @Override public void setParentSheet(PropertySheet parentSheet) {
            m_parentSheet = parentSheet;
        }
    }

    @JsonPropertyOrder(alphabetic=true)
    static class NestedPropertySheetImpl
        extends AbstractProperty<PropertySheet>
        implements NestedPropertySheet
    {
        private PropertySheet m_propertySheet;

        protected NestedPropertySheetImpl(String name,
                PropertySheet propertySheet)
        {
            super(name);
            m_propertySheet = propertySheet;
        }

        NestedPropertySheetImpl() { }

        @Override public PropertySheet getValue() {
            return m_propertySheet;
        }

        @Override public void setValue(PropertySheet propertySheet) {
            m_propertySheet = propertySheet;
        }
    }

    @SuppressWarnings("rawtypes")
    static class PropertySheetImpl
        extends AbstractEntity
        implements PropertySheet
    {
        private Map<String, Property> m_properties;

        @Override public void addProperty(Property property)
        {
            if (m_properties == null) {
                m_properties = new TreeMap<String, Property>();
            }
            property.setParentSheet(this);
            m_properties.put(property.getName(), property);
        }

        @JsonDeserialize(as = TreeMap.class,
            keyAs     = String.class,
            contentAs = Property.class)
        @JsonManagedReference("propertySheet-properties")
        @Override public Map<String, Property> getProperties() {
            return m_properties;
        }

        @Override public void setProperties(Map<String, Property> properties) {
            m_properties = properties;
        }
    }

    static class StringPropertyImpl
        extends AbstractProperty<String>
        implements StringProperty
    {
        private String m_value;

        protected StringPropertyImpl(String name, String value) {
            super(name);
            m_value = value;
        }

        StringPropertyImpl() { }

        @Override public String getValue() {
            return m_value;
        }

        @Override public void setValue(String value) {
            m_value = value;
        }
    }

    static class YetAnotherClass extends StringPropertyImpl { }

    // [databind#3304]
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleChild3304.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = RefChild3304.class, name = "REF")
    })
    interface IChild3304 { }

    static class SimpleChild3304 implements IChild3304 {
        public String value;

        public SimpleChild3304() { }
        public SimpleChild3304(String v) { value = v; }
    }

    static class RefChild3304 implements IChild3304 {
        public String data;

        @JsonBackReference
        public Container3304 parent;

        public RefChild3304() { }
        public RefChild3304(String d) { data = d; }
    }

    static class Container3304 {
        @JsonManagedReference
        public IChild3304[] children;
    }

    static class ContainerWithList3304 {
        @JsonManagedReference
        public java.util.List<IChild3304> children;
    }

    // [databind#1516]
    static class ParentWithCreator
    {
        String id, name;

        @JsonManagedReference
        ChildObject1 child;

        @ConstructorProperties({"id", "name", "child"})
        public ParentWithCreator(String id, String name, ChildObject1 child) {
            this.id = id;
            this.name = name;
            this.child = child;
        }
    }

    static class ChildObject1
    {
        public String id, name;

        @JsonBackReference
        public ParentWithCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject1(String id, String name, ParentWithCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    static class ParentWithoutCreator {
        public String id, name;

        @JsonManagedReference
        public ChildObject2 child;
    }

    static class ChildObject2 {
        public String id, name;

        @JsonBackReference
        public ParentWithoutCreator parent;

        @ConstructorProperties({"id", "name", "parent"})
        public ChildObject2(String id, String name,
                            ParentWithoutCreator parent) {
            this.id = id;
            this.name = name;
            this.parent = parent;
        }
    }

    // [Kotlin#129]
    static class Car {
        private final long id;

        @JsonManagedReference
        private final List<Color> colors;

        @JsonCreator
        public Car(@JsonProperty("id") long id,
                   @JsonProperty("colors") List<Color> colors) {
            this.id = id;
            this.colors = colors != null ? colors : new ArrayList<>();
        }

        public long getId() { return id; }
        public List<Color> getColors() { return colors; }
    }

    static class Color {
        private final long id;
        private final String code;

        @JsonBackReference
        private Car car;

        @JsonCreator
        public Color(@JsonProperty("id") long id,
                     @JsonProperty("code") String code) {
            this.id = id;
            this.code = code;
        }

        public long getId() { return id; }
        public String getCode() { return code; }
        public Car getCar() { return car; }
        public void setCar(Car car) { this.car = car; }
    }

    // [databind#2686]
    public static class Container2686 {
        Content2686 forward;

        String containerValue;

        @JsonManagedReference
        public Content2686 getForward() {
            return forward;
        }

        @JsonManagedReference
        public void setForward(Content2686 forward) {
            this.forward = forward;
        }

        public String getContainerValue() {
            return containerValue;
        }

        public void setContainerValue(String containerValue) {
            this.containerValue = containerValue;
        }
    }

    @JsonDeserialize(builder = Content2686.Builder.class)
    public static class Content2686 {
        private Container2686 back;

        private String contentValue;

        public Content2686(Container2686 back, String contentValue) {
            this.back = back;
            this.contentValue = contentValue;
        }

        public String getContentValue() {
            return contentValue;
        }

        @JsonBackReference
        public Container2686 getBack() {
            return back;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private Container2686 back;
            private String contentValue;

            @JsonBackReference
            Builder back(Container2686 b) {
                this.back = b;
                return this;
            }

            Builder contentValue(String cv) {
                this.contentValue = cv;
                return this;
            }

            Content2686 build() {
                return new Content2686(back, contentValue);
            }
        }
    }

    /*
    /**********************************************************
    /* Unit tests (TestParentChildReferences)
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleRefs() throws Exception
    {
        SimpleTreeNode root = new SimpleTreeNode("root");
        SimpleTreeNode child = new SimpleTreeNode("kid");
        root.child = child;
        child.parent = root;

        String json = MAPPER.writeValueAsString(root);

        SimpleTreeNode resultNode = MAPPER.readValue(json, SimpleTreeNode.class);
        assertEquals("root", resultNode.name);
        SimpleTreeNode resultChild = resultNode.child;
        assertNotNull(resultChild);
        assertEquals("kid", resultChild.name);
        assertSame(resultChild.parent, resultNode);
    }

    // [JACKSON-693]
    @Test
    public void testSimpleRefsWithGetter() throws Exception
    {
        SimpleTreeNode2 root = new SimpleTreeNode2("root");
        SimpleTreeNode2 child = new SimpleTreeNode2("kid");
        root.child = child;
        child.parent = root;

        String json = MAPPER.writeValueAsString(root);

        SimpleTreeNode2 resultNode = MAPPER.readValue(json, SimpleTreeNode2.class);
        assertEquals("root", resultNode.name);
        SimpleTreeNode2 resultChild = resultNode.child;
        assertNotNull(resultChild);
        assertEquals("kid", resultChild.name);
        assertSame(resultChild.parent, resultNode);
    }

    @Test
    public void testFullRefs() throws Exception
    {
        FullTreeNode root = new FullTreeNode("root");
        FullTreeNode child1 = new FullTreeNode("kid1");
        FullTreeNode child2 = new FullTreeNode("kid2");
        root.firstChild = child1;
        child1.parent = root;
        child1.next = child2;
        child2.prev = child1;

        String json = MAPPER.writeValueAsString(root);

        FullTreeNode resultNode = MAPPER.readValue(json, FullTreeNode.class);
        assertEquals("root", resultNode.name);
        FullTreeNode resultChild = resultNode.firstChild;
        assertNotNull(resultChild);
        assertEquals("kid1", resultChild.name);
        assertSame(resultChild.parent, resultNode);

        // and then sibling linkage
        assertNull(resultChild.prev);
        FullTreeNode resultChild2 = resultChild.next;
        assertNotNull(resultChild2);
        assertEquals("kid2", resultChild2.name);
        assertSame(resultChild, resultChild2.prev);
        assertNull(resultChild2.next);
    }

    @Test
    public void testArrayOfRefs() throws Exception
    {
        NodeArray root = new NodeArray();
        ArrayNode node1 = new ArrayNode("a");
        ArrayNode node2 = new ArrayNode("b");
        root.nodes = new ArrayNode[] { node1, node2 };
        String json = MAPPER.writeValueAsString(root);

        NodeArray result = MAPPER.readValue(json, NodeArray.class);
        ArrayNode[] kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.length);
        assertEquals("a", kids[0].name);
        assertEquals("b", kids[1].name);
        assertSame(result, kids[0].parent);
        assertSame(result, kids[1].parent);
    }

    @Test
    public void testListOfRefs() throws Exception
    {
        NodeList root = new NodeList();
        NodeForList node1 = new NodeForList("a");
        NodeForList node2 = new NodeForList("b");
        root.nodes = Arrays.asList(node1, node2);
        String json = MAPPER.writeValueAsString(root);

        NodeList result = MAPPER.readValue(json, NodeList.class);
        List<NodeForList> kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.size());
        assertEquals("a", kids.get(0).name);
        assertEquals("b", kids.get(1).name);
        assertSame(result, kids.get(0).parent);
        assertSame(result, kids.get(1).parent);
    }

    @Test
    public void testMapOfRefs() throws Exception
    {
        NodeMap root = new NodeMap();
        NodeForMap node1 = new NodeForMap("a");
        NodeForMap node2 = new NodeForMap("b");
        Map<String,NodeForMap> nodes = new HashMap<String, NodeForMap>();
        nodes.put("a1", node1);
        nodes.put("b2", node2);
        root.nodes = nodes;
        String json = MAPPER.writeValueAsString(root);

        NodeMap result = MAPPER.readValue(json, NodeMap.class);
        Map<String,NodeForMap> kids = result.nodes;
        assertNotNull(kids);
        assertEquals(2, kids.size());
        assertNotNull(kids.get("a1"));
        assertNotNull(kids.get("b2"));
        assertEquals("a", kids.get("a1").name);
        assertEquals("b", kids.get("b2").name);
        assertSame(result, kids.get("a1").parent);
        assertSame(result, kids.get("b2").parent);
    }

    // for [JACKSON-368]
    @Test
    public void testAbstract368() throws Exception
    {
        AbstractNode parent = new ConcreteNode("p");
        AbstractNode child = new ConcreteNode("c");
        parent.next = child;
        child.prev = parent;

        // serialization ought to be ok
        String json = MAPPER.writeValueAsString(parent);

        AbstractNode root = MAPPER.readValue(json, AbstractNode.class);

        assertEquals(ConcreteNode.class, root.getClass());
        assertEquals("p", root.id);
        assertNull(root.prev);
        AbstractNode leaf = root.next;
        assertNotNull(leaf);
        assertEquals("c", leaf.id);
        assertSame(root, leaf.prev);
    }

    @Test
    public void testIssue693() throws Exception
    {
        Parent parent = new Parent();
        parent.addChild(new Child("foo"));
        parent.addChild(new Child("bar"));
        byte[] bytes = MAPPER.writeValueAsBytes(parent);
        Parent value = MAPPER.readValue(bytes, Parent.class);
        for (Child child : value.children) {
            assertEquals(value, child.getParent());
        }
    }

    @Test
    public void testIssue708() throws Exception
    {
        Advertisement708 ad = MAPPER.readValue("{\"title\":\"Hroch\",\"photos\":[{\"id\":3}]}", Advertisement708.class);
        assertNotNull(ad);
    }

    // [databind#1878]
    @Test
    public void testChildDeserialization() throws Exception {
        Child1878 child = MAPPER.readValue("{\"b\": {}}", Child1878.class);
        assertNotNull(child.b);
    }

    // Forward reference with @JsonIdentityInfo
    @Test
    public void testForwardRef() throws IOException {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        final String outerTypeName = getClass().getSimpleName();
        mapper.readValue("{" +
                "  \"@type\" : \"" + outerTypeName + "$ForwardReferenceContainerClass\"," +
                "  \"frc\" : \"willBeForwardReferenced\"," +
                "  \"yac\" : {" +
                "    \"@type\" : \"" + outerTypeName + "$YetAnotherClass2\"," +
                "    \"frc\" : {" +
                "      \"@type\" : \"One\"," +
                "      \"id\" : \"willBeForwardReferenced\"" +
                "    }," +
                "    \"id\" : \"anId\"" +
                "  }," +
                "  \"id\" : \"ForwardReferenceContainerClass1\"" +
                "}", ForwardReferenceContainerClass.class);
    }

    /*
    /**********************************************************
    /* Unit tests, misc back-reference edge cases (BackReferenceMiscTest)
    /**********************************************************
     */

    // NOTE: order is arbitrary, test is fragile... has to work for now
    private final String CLASS_NAME = getClass().getName();
    private final String JSON =
        "{\"@class\":\""+CLASS_NAME+"$PropertySheetImpl\",\"id\":0,\"properties\":{\"p1name\":{\"@class\":"
            +"\"" +CLASS_NAME+ "$StringPropertyImpl\",\"id\":0,\"name\":\"p1name\",\"value\":\"p1value\"},"
            +"\"p2name\":{\"@class\":\""+CLASS_NAME+"$StringPropertyImpl\",\"id\":0,"
            +"\"name\":\"p2name\",\"value\":\"p2value\"}}}";

    @Test
    public void testDeserialize() throws IOException
    {
        PropertySheet input = MAPPER.readValue(JSON, PropertySheet.class);
        assertEquals(JSON, MAPPER.writeValueAsString(input));
    }

    @Test
    public void testSerialize() throws IOException
    {
        PropertySheet sheet = new PropertySheetImpl();

        sheet.addProperty(new StringPropertyImpl("p1name", "p1value"));
        sheet.addProperty(new StringPropertyImpl("p2name", "p2value"));
        String actual = MAPPER.writeValueAsString(sheet);
        assertEquals(JSON, actual);
    }

    // [databind#3304]: Verify error message mentions the abstract type issue
    @Test
    public void testBackRefOnSubtypeOnly_array() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        Container3304.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }

    // [databind#3304]
    @Test
    public void testBackRefOnSubtypeOnly_list() {
        InvalidDefinitionException ex = assertThrows(
                InvalidDefinitionException.class,
                () -> MAPPER.readValue(
                        "{\"children\":[{\"type\":\"SIMPLE\",\"value\":\"x\"}]}",
                        ContainerWithList3304.class));
        String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("abstract"),
                "Error message should mention abstract type issue, got: " + msg);
        assertTrue(msg.contains("@JsonBackReference"),
                "Error message should mention @JsonBackReference, got: " + msg);
    }

    // [databind#1516]
    @Test
    public void testWithParentCreator() throws Exception {
        String json = a2q(
                "{ 'id': 'abc',\n" +
                "  'name': 'Bob',\n" +
                "  'child': { 'id': 'def', 'name':'Bert' }\n" +
                "}");
        ParentWithCreator result = MAPPER.readValue(json, ParentWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    // [databind#1516]
    @Test
    public void testWithParentNoCreator() throws Exception {
        String json = a2q(
                "{ 'id': 'abc',\n" +
                "  'name': 'Bob',\n" +
                "  'child': { 'id': 'def', 'name':'Bert' }\n" +
                "}");
        ParentWithoutCreator result = MAPPER.readValue(json, ParentWithoutCreator.class);
        assertNotNull(result);
        assertNotNull(result.child);
        assertSame(result, result.child.parent);
    }

    // [Kotlin#129]
    @Test
    public void testManagedReferenceOnCreator() throws Exception
    {
        Car car = new Car(100, new ArrayList<>());
        Color color = new Color(100, "#FFFFF");
        color.setCar(car);
        car.getColors().add(color);

        String json = MAPPER.writeValueAsString(car);
        Car result = MAPPER.readValue(json, Car.class);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertNotNull(result.getColors());
        assertEquals(1, result.getColors().size());

        Color resultColor = result.getColors().get(0);
        assertEquals(100, resultColor.getId());
        assertEquals("#FFFFF", resultColor.getCode());

        assertNotNull(resultColor.getCar());
        assertSame(result, resultColor.getCar());
    }

    // [databind#2686]
    @Test
    public void testBuildWithBackRefs2686() throws Exception {
        Container2686 container = new Container2686();
        container.containerValue = "containerValue";
        Content2686 content = new Content2686(container, "contentValue");
        container.forward = content;

        String json = MAPPER.writeValueAsString(container);
        Container2686 result = MAPPER.readValue(json, Container2686.class);

        assertNotNull(result);
        assertNotNull(result.getForward());
        assertEquals("contentValue", result.getForward().getContentValue());
    }
}

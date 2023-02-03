package com.fasterxml.jackson.databind.struct;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.*;

public class TestParentChildReferences
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes
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
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();

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

    public void testIssue708() throws Exception
    {
        Advertisement708 ad = MAPPER.readValue("{\"title\":\"Hroch\",\"photos\":[{\"id\":3}]}", Advertisement708.class);
        assertNotNull(ad);
    }
}

package tools.jackson.databind.objectid;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6359]: self-referencing type with property-level `@JsonIdentityInfo`:
// nested value must get the same property settings as the root value
public class ObjectIdSelfReferenceResolve6359Test extends DatabindTestUtil
{
    @JsonPropertyOrder({"id", "next", "z"})
    static class Node {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public Node next;
        @JsonSetter(nulls = Nulls.FAIL)
        public String z;
    }

    @JsonPropertyOrder({"id", "next", "child"})
    static class RefNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public RefNode next;
        @JsonManagedReference
        public RefChild child;
    }

    static class RefChild {
        public String name;
        @JsonBackReference
        public RefNode parent;
    }

    @JsonPropertyOrder({"id", "next", "x", "z"})
    static class IgnoralNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        @JsonIgnoreProperties({"x"})
        public IgnoralNode next;
        public String x;
        @JsonSetter(nulls = Nulls.FAIL)
        public String z;
    }

    @JsonPropertyOrder({"id", "next", "z"})
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class ArrayNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public ArrayNode next;
        @JsonSetter(nulls = Nulls.FAIL)
        public String z;
    }

    @JsonDeserialize(builder = BuiltNode.Builder.class)
    static class BuiltNode {
        final int id;
        final BuiltNode next;
        final String z;

        BuiltNode(int id, BuiltNode next, String z) {
            this.id = id;
            this.next = next;
            this.z = z;
        }

        @JsonPOJOBuilder(withPrefix = "")
        @JsonPropertyOrder({"id", "next", "z"})
        static class Builder {
            int id;
            BuiltNode next;
            String z;

            public Builder id(int id) { this.id = id; return this; }

            @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
            public Builder next(BuiltNode next) { this.next = next; return this; }

            @JsonSetter(nulls = Nulls.FAIL)
            public Builder z(String z) { this.z = z; return this; }

            public BuiltNode build() { return new BuiltNode(id, next, z); }
        }
    }

    // Indirect cycle: A -> B -> A, with Object Id on B.a
    @JsonPropertyOrder({"id", "b", "z"})
    static class CycleA {
        public int id;
        public CycleB b;
        @JsonSetter(nulls = Nulls.FAIL)
        public String z;
    }

    static class CycleB {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public CycleA a;
    }

    // Creator property ignored for nested value only
    static class CreatorNode {
        public final int id;
        public final String name;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        @JsonIgnoreProperties({"name"})
        public CreatorNode next;

        @JsonCreator
        CreatorNode(@JsonProperty("id") int id, @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    @JsonPropertyOrder({"id", "next", "z"})
    static class CaseInsensitiveNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        public CaseInsensitiveNode next;
        @JsonSetter(nulls = Nulls.FAIL)
        public String z;
    }

    @JsonPropertyOrder({"id", "next"})
    static class AnySetterNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public AnySetterNode next;

        final Map<String, Object> other = new LinkedHashMap<>();

        @JsonAnySetter
        public void set(String key, Object value) { other.put(key, value); }
    }

    static class UnwrappedValue {
        public String a, b;
    }

    @JsonPropertyOrder({"id", "next", "value"})
    static class UnwrappedNode {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public UnwrappedNode next;
        @JsonUnwrapped
        public UnwrappedValue value;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void nullsFailInNestedValue() throws Exception
    {
        // root value: fails as expected
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"z":null}
                        """, Node.class));
        // nested value: should fail the same way
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"next":{"id":2,"z":null}}
                        """, Node.class));
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"next":{"id":2,"next":{"id":3,"z":null}}}
                        """, Node.class));
    }

    @Test
    void nestedValueWithoutNulls() throws Exception
    {
        Node node = MAPPER.readValue("""
                {"id":1,"next":{"id":2,"z":"b"},"z":"a"}
                """, Node.class);
        assertEquals(1, node.id);
        assertEquals("a", node.z);
        assertNotNull(node.next);
        assertEquals(2, node.next.id);
        assertEquals("b", node.next.z);
    }

    @Test
    void managedReferenceInNestedValue() throws Exception
    {
        RefNode node = MAPPER.readValue("""
                {"id":1,"next":{"id":2,"child":{"name":"b"}},"child":{"name":"a"}}
                """, RefNode.class);
        assertSame(node, node.child.parent);
        assertNotNull(node.next);
        assertEquals(2, node.next.id);
        assertEquals("b", node.next.child.name);
        assertSame(node.next, node.next.child.parent);
    }

    // Per-property ignorals of the contextual copy must be kept
    @Test
    void ignoralsInNestedValue() throws Exception
    {
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"next":{"id":2,"x":"q","z":null}}
                        """, IgnoralNode.class));
        IgnoralNode node = MAPPER.readValue("""
                {"id":1,"next":{"id":2,"x":"q","z":"b"},"x":"p"}
                """, IgnoralNode.class);
        assertEquals("p", node.x);
        assertNull(node.next.x);
        assertEquals("b", node.next.z);
    }

    @Test
    void arrayShapeNestedValue() throws Exception
    {
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        [1,[2,null,null],"a"]
                        """, ArrayNode.class));
        ArrayNode node = MAPPER.readValue("""
                [1,[2,null,"b"],"a"]
                """, ArrayNode.class);
        assertEquals(2, node.next.id);
        assertEquals("b", node.next.z);
    }

    @Test
    void builderNestedValue() throws Exception
    {
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"next":{"id":2,"z":null}}
                        """, BuiltNode.class));
        BuiltNode node = MAPPER.readValue("""
                {"id":1,"next":{"id":2,"z":"b"},"z":"a"}
                """, BuiltNode.class);
        assertEquals(2, node.next.id);
        assertEquals("b", node.next.z);
    }

    @Test
    void indirectCycleNestedValue() throws Exception
    {
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"b":{"a":{"id":2,"z":null}}}
                        """, CycleA.class));
    }

    // Per-property ignorals must also apply to Creator properties of the copy
    @Test
    void creatorIgnoralsInNestedValue() throws Exception
    {
        CreatorNode node = MAPPER.readValue("""
                {"id":1,"name":"a","next":{"id":2,"name":"b"}}
                """, CreatorNode.class);
        assertEquals("a", node.name);
        assertEquals(2, node.next.id);
        assertNull(node.next.name);
    }

    @Test
    void caseInsensitiveNestedValue() throws Exception
    {
        CaseInsensitiveNode node = MAPPER.readValue("""
                {"id":1,"next":{"ID":2,"Z":"b"}}
                """, CaseInsensitiveNode.class);
        assertEquals(2, node.next.id);
        assertEquals("b", node.next.z);
        assertThrows(InvalidNullException.class,
                () -> MAPPER.readValue("""
                        {"id":1,"next":{"ID":2,"Z":null}}
                        """, CaseInsensitiveNode.class));
    }

    @Test
    void anySetterInNestedValue() throws Exception
    {
        AnySetterNode node = MAPPER.readValue("""
                {"id":1,"a":1,"next":{"id":2,"b":2}}
                """, AnySetterNode.class);
        assertEquals(Map.of("a", 1), node.other);
        assertEquals(2, node.next.id);
        assertEquals(Map.of("b", 2), node.next.other);
    }

    @Test
    void unwrappedInNestedValue() throws Exception
    {
        UnwrappedNode node = MAPPER.readValue("""
                {"id":1,"a":"x","next":{"id":2,"a":"y","b":"z"}}
                """, UnwrappedNode.class);
        assertEquals("x", node.value.a);
        assertEquals(2, node.next.id);
        assertNotNull(node.next.value);
        assertEquals("y", node.next.value.a);
        assertEquals("z", node.next.value.b);
    }
}

package tools.jackson.databind.jsontype.deduct;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested values with deduction-based polymorphic handling,
 * where inner values are read from buffered content.
 */
public class NestedDeductionTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({ @JsonSubTypes.Type(Group.class), @JsonSubTypes.Type(Leaf.class) })
    static abstract class Node {
        public String name;
    }

    static class Group extends Node {
        public List<Node> children;
    }

    static class Leaf extends Node {
        public int[] data;
    }

    // Ambiguous subtypes, with default impl
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = DefaultShape.class)
    @JsonSubTypes({ @JsonSubTypes.Type(Square.class), @JsonSubTypes.Type(Circle.class) })
    static abstract class Shape {
        public List<Shape> parts;
    }

    static class Square extends Shape {
        public int side;
    }

    static class Circle extends Shape {
        public int radius;
    }

    static class DefaultShape extends Shape { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void sharedPropertyFirst() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'name':'root','children':["
                + "{'name':'a','data':[1,2]},"
                + "{'name':'b','children':[{'name':'c','data':[3]}]}"
                + "]}"), Node.class));
    }

    @Test
    void sharedPropertyLast() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'children':["
                + "{'data':[1,2],'name':'a'},"
                + "{'children':[{'data':[3],'name':'c'}],'name':'b'}"
                + "],'name':'root'}"), Node.class));
    }

    private void _verify(Node n)
    {
        Group root = (Group) n;
        assertEquals("root", root.name);
        assertEquals(2, root.children.size());
        Leaf a = (Leaf) root.children.get(0);
        assertEquals("a", a.name);
        assertArrayEquals(new int[] { 1, 2 }, a.data);
        Group b = (Group) root.children.get(1);
        assertEquals("b", b.name);
        Leaf c = (Leaf) b.children.get(0);
        assertEquals("c", c.name);
        assertArrayEquals(new int[] { 3 }, c.data);
    }

    @Test
    void deepNesting() throws Exception
    {
        final int depth = 200;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{'name':'n").append(i).append("','children':[");
        }
        sb.append("{'name':'leaf','data':[42]}");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        Node n = MAPPER.readValue(a2q(sb.toString()), Node.class);
        for (int i = 0; i < depth; i++) {
            Group g = (Group) n;
            assertEquals("n" + i, g.name);
            assertEquals(1, g.children.size());
            n = g.children.get(0);
        }
        assertArrayEquals(new int[] { 42 }, ((Leaf) n).data);
    }

    // Nested values that cannot be deduced use default impl
    @Test
    void nestedDefaultImpl() throws Exception
    {
        Shape s = MAPPER.readValue(a2q(
                "{'parts':[{'parts':[]},{'parts':[],'side':3},{'radius':2,'parts':[{'parts':[]}]}],'side':1}"),
                Shape.class);
        assertEquals(Square.class, s.getClass());
        assertEquals(1, ((Square) s).side);
        assertEquals(3, s.parts.size());
        assertEquals(DefaultShape.class, s.parts.get(0).getClass());
        assertEquals(3, ((Square) s.parts.get(1)).side);
        Circle c = (Circle) s.parts.get(2);
        assertEquals(2, c.radius);
        assertEquals(DefaultShape.class, c.parts.get(0).getClass());
    }

    // Nested values that cannot be deduced (no default impl) fail
    @Test
    void nestedCannotDeduce() throws Exception
    {
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> MAPPER.readValue(a2q(
                        "{'children':[{'name':'x'}],'name':'root'}"), Node.class));
        verifyException(e, "Cannot deduce unique subtype");
    }

    @Test
    void nestedCaseInsensitive() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        Node n = mapper.readValue(a2q(
                "{'Name':'root','CHILDREN':[{'NAME':'a','Data':[1]}]}"), Node.class);
        Group root = (Group) n;
        assertEquals("root", root.name);
        assertArrayEquals(new int[] { 1 }, ((Leaf) root.children.get(0)).data);
    }
}

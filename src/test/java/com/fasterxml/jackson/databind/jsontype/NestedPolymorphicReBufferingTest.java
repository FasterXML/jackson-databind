package com.fasterxml.jackson.databind.jsontype;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [databind#6482]: nested polymorphic values that need buffering
 * (type id after other properties; deduction) must still be read correctly
 * when buffered content is re-buffered.
 */
public class NestedPolymorphicReBufferingTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = Group.class, name = "group"),
        @JsonSubTypes.Type(value = Leaf.class, name = "leaf") })
    static abstract class Node { }

    static class Group extends Node {
        public String name;
        public List<Node> children;
    }

    static class Leaf extends Node {
        public int[] data;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "type", visible = true)
    @JsonSubTypes({ @JsonSubTypes.Type(value = VisibleGroup.class, name = "group"),
        @JsonSubTypes.Type(value = VisibleLeaf.class, name = "leaf") })
    static abstract class VisibleNode {
        public String type;
    }

    static class VisibleGroup extends VisibleNode {
        public String name;
        public List<VisibleNode> children;
    }

    static class VisibleLeaf extends VisibleNode {
        public int[] data;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({ @JsonSubTypes.Type(DeducedGroup.class), @JsonSubTypes.Type(DeducedLeaf.class) })
    static abstract class DeducedNode { }

    static class DeducedGroup extends DeducedNode {
        public List<DeducedNode> children;
        public int level;
    }

    static class DeducedLeaf extends DeducedNode {
        public int[] data;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final static int DEPTH = 100;
    private final static int LEAF_LENGTH = 1000;

    @Test
    public void typeIdLast() throws Exception
    {
        String json = _build(DEPTH, LEAF_LENGTH, "", ",'type':'group'", ",'type':'leaf'");
        Node n = MAPPER.readValue(json, Node.class);
        for (int i = 0; i < DEPTH; ++i) {
            assertEquals(Group.class, n.getClass());
            Group g = (Group) n;
            assertEquals("g"+i, g.name);
            assertEquals(2, g.children.size());
            assertEquals(Leaf.class, g.children.get(1).getClass());
            assertArrayEquals(new int[] { i }, ((Leaf) g.children.get(1)).data);
            n = g.children.get(0);
        }
        assertEquals(Leaf.class, n.getClass());
        _verifyLeaf(((Leaf) n).data);
    }

    @Test
    public void visibleTypeIdLast() throws Exception
    {
        String json = _build(DEPTH, LEAF_LENGTH, "", ",'type':'group'", ",'type':'leaf'");
        VisibleNode n = MAPPER.readValue(json, VisibleNode.class);
        for (int i = 0; i < DEPTH; ++i) {
            assertEquals(VisibleGroup.class, n.getClass());
            assertEquals("group", n.type);
            VisibleGroup g = (VisibleGroup) n;
            assertEquals(2, g.children.size());
            assertEquals("leaf", g.children.get(1).type);
            n = g.children.get(0);
        }
        assertEquals(VisibleLeaf.class, n.getClass());
        assertEquals("leaf", n.type);
        _verifyLeaf(((VisibleLeaf) n).data);
    }

    @Test
    public void deduction() throws Exception
    {
        // Note: no "name" property here (not known by DeducedGroup)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DEPTH; ++i) {
            sb.append("{'children':[");
        }
        sb.append("{'data':").append(_leafArray(LEAF_LENGTH)).append("}");
        for (int i = DEPTH; --i >= 0; ) {
            sb.append(",{'data':[").append(i).append("]}],'level':").append(i).append('}');
        }
        DeducedNode n = MAPPER.readValue(a2q(sb.toString()), DeducedNode.class);
        for (int i = 0; i < DEPTH; ++i) {
            assertEquals(DeducedGroup.class, n.getClass());
            DeducedGroup g = (DeducedGroup) n;
            assertEquals(i, g.level);
            assertEquals(2, g.children.size());
            assertArrayEquals(new int[] { i }, ((DeducedLeaf) g.children.get(1)).data);
            n = g.children.get(0);
        }
        assertEquals(DeducedLeaf.class, n.getClass());
        _verifyLeaf(((DeducedLeaf) n).data);
    }

    // Structure: Group with "name" and "children" (nested Group, and a small Leaf)
    private static String _build(int depth, int leafLength,
            String typeFirst, String groupTypeLast, String leafTypeLast)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; ++i) {
            sb.append("{").append(typeFirst).append("'name':'g").append(i).append("','children':[");
        }
        sb.append("{'data':").append(_leafArray(leafLength)).append(leafTypeLast).append("}");
        for (int i = depth; --i >= 0; ) {
            sb.append(",{'data':[").append(i).append("]").append(leafTypeLast).append("}]")
                .append(groupTypeLast).append('}');
        }
        return a2q(sb.toString());
    }

    private static String _leafArray(int length) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        return sb.append(']').toString();
    }

    private static void _verifyLeaf(int[] data) {
        assertEquals(LEAF_LENGTH, data.length);
        for (int i = 0; i < LEAF_LENGTH; ++i) {
            assertEquals(i, data[i]);
        }
    }
}

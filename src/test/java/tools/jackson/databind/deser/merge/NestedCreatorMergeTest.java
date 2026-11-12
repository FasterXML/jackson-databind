package tools.jackson.databind.deser.merge;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for merging properties of POJOs created using property-based creator,
 * nested so that inner values are read from buffered content.
 */
public class NestedCreatorMergeTest extends DatabindTestUtil
{
    static class Node {
        final String id;

        @JsonMerge
        public List<Node> children = new ArrayList<>();

        @JsonMerge
        public List<String> tags = new ArrayList<>(List.of("default"));

        @JsonMerge
        public Settings settings = new Settings();

        @JsonCreator
        Node(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

    static class Settings {
        public int a = 1;
        public int b = 2;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void creatorPropertyFirst() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'id':'root','tags':['r'],'children':["
                + "{'id':'a','settings':{'b':5},'children':[{'id':'c','tags':['x','y']}]},"
                + "{'id':'b','tags':[],'children':[]}"
                + "],'settings':{'a':3}}"), Node.class));
    }

    @Test
    void creatorPropertyLast() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'tags':['r'],'children':["
                + "{'settings':{'b':5},'children':[{'tags':['x','y'],'id':'c'}],'id':'a'},"
                + "{'tags':[],'children':[],'id':'b'}"
                + "],'settings':{'a':3},'id':'root'}"), Node.class));
    }

    private void _verify(Node root)
    {
        assertEquals("root", root.id);
        assertEquals(List.of("default", "r"), root.tags);
        assertEquals(3, root.settings.a);
        assertEquals(2, root.settings.b);
        assertEquals(2, root.children.size());

        Node a = root.children.get(0);
        assertEquals("a", a.id);
        assertEquals(List.of("default"), a.tags);
        assertEquals(1, a.settings.a);
        assertEquals(5, a.settings.b);
        assertEquals(1, a.children.size());

        Node c = a.children.get(0);
        assertEquals("c", c.id);
        assertEquals(List.of("default", "x", "y"), c.tags);
        assertEquals(0, c.children.size());

        Node b = root.children.get(1);
        assertEquals("b", b.id);
        assertEquals(List.of("default"), b.tags);
        assertEquals(0, b.children.size());
    }

    @Test
    void deepNesting() throws Exception
    {
        final int depth = 200;
        for (boolean idLast : new boolean[] { false, true }) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                if (idLast) {
                    sb.append("{'children':[");
                } else {
                    sb.append("{'id':'n").append(i).append("','children':[");
                }
            }
            sb.append("{'id':'leaf','tags':['t']}");
            for (int i = 0; i < depth; i++) {
                if (idLast) {
                    sb.append("],'id':'n").append(depth - 1 - i).append("'}");
                } else {
                    sb.append("]}");
                }
            }
            Node n = MAPPER.readValue(a2q(sb.toString()), Node.class);
            for (int i = 0; i < depth; i++) {
                assertEquals("n" + i, n.id);
                assertEquals(1, n.children.size());
                n = n.children.get(0);
            }
            assertEquals("leaf", n.id);
            assertEquals(List.of("default", "t"), n.tags);
        }
    }
}

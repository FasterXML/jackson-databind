package tools.jackson.databind.jsontype.ext;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested values with external type ids, with type id
 * before or after the value (in which case value has to be buffered
 * and nested values are read from buffered content).
 */
public class NestedExternalTypeIdTest extends DatabindTestUtil
{
    static class Holder {
        public String name;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = Group.class, name = "group"),
            @JsonSubTypes.Type(value = Leaf.class, name = "leaf") })
        public Object value;
    }

    static class Group {
        public List<Holder> children;
        public String label;
    }

    static class Leaf {
        public int[] data;
    }

    static class CreatorHolder {
        final String type;
        final Object value;

        @JsonCreator
        CreatorHolder(@JsonProperty("type") String type,
                @JsonProperty("value")
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "type")
                @JsonSubTypes({ @JsonSubTypes.Type(value = CreatorGroup.class, name = "group"),
                    @JsonSubTypes.Type(value = Leaf.class, name = "leaf") })
                Object value) {
            this.type = type;
            this.value = value;
        }
    }

    static class CreatorGroup {
        public List<CreatorHolder> children;
        public String label;
    }

    // Type id is also a regular property
    static class VisibleHolder {
        public String type;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "type")
        @JsonSubTypes({ @JsonSubTypes.Type(value = VisibleGroup.class, name = "group"),
            @JsonSubTypes.Type(value = Leaf.class, name = "leaf") })
        public Object value;
    }

    static class VisibleGroup {
        public List<VisibleHolder> children;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void typeIdFirst() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'name':'root','type':'group','value':{'children':["
                + "{'type':'leaf','value':{'data':[1,2]},'name':'a'},"
                + "{'type':'group','value':{'children':[{'type':'leaf','value':{'data':[3]}}],'label':'in'}},"
                + "{'type':'leaf','value':null}"
                + "],'label':'top'}}"), Holder.class));
    }

    @Test
    void typeIdLast() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'name':'root','value':{'children':["
                + "{'value':{'data':[1,2]},'name':'a','type':'leaf'},"
                + "{'value':{'children':[{'value':{'data':[3]},'type':'leaf'}],'label':'in'},'type':'group'},"
                + "{'value':null,'type':'leaf'}"
                + "],'label':'top'},'type':'group'}"), Holder.class));
    }

    @Test
    void typeIdMixed() throws Exception
    {
        _verify(MAPPER.readValue(a2q(
                "{'name':'root','value':{'children':["
                + "{'type':'leaf','value':{'data':[1,2]},'name':'a'},"
                + "{'value':{'children':[{'type':'leaf','value':{'data':[3]}}],'label':'in'},'type':'group'},"
                + "{'type':'leaf','value':null}"
                + "],'label':'top'},'type':'group'}"), Holder.class));
    }

    private void _verify(Holder root)
    {
        assertEquals("root", root.name);
        Group top = (Group) root.value;
        assertEquals("top", top.label);
        assertEquals(3, top.children.size());

        Holder a = top.children.get(0);
        assertEquals("a", a.name);
        assertArrayEquals(new int[] { 1, 2 }, ((Leaf) a.value).data);

        Group in = (Group) top.children.get(1).value;
        assertEquals("in", in.label);
        assertEquals(1, in.children.size());
        assertArrayEquals(new int[] { 3 }, ((Leaf) in.children.get(0).value).data);

        assertNull(top.children.get(2).value);
    }

    @Test
    void deepNesting() throws Exception
    {
        final int depth = 100;
        for (boolean typeLast : new boolean[] { false, true }) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append(typeLast ? "{'value':{'children':[" : "{'type':'group','value':{'children':[");
            }
            sb.append(typeLast ? "{'value':{'data':[42]},'type':'leaf'}" : "{'type':'leaf','value':{'data':[42]}}");
            for (int i = 0; i < depth; i++) {
                sb.append("],'label':'g").append(i)
                    .append(typeLast ? "'},'type':'group'}" : "'}}");
            }
            Holder h = MAPPER.readValue(a2q(sb.toString()), Holder.class);
            for (int i = depth - 1; i >= 0; i--) {
                Group g = (Group) h.value;
                assertEquals("g" + i, g.label);
                assertEquals(1, g.children.size());
                h = g.children.get(0);
            }
            assertArrayEquals(new int[] { 42 }, ((Leaf) h.value).data);
        }
    }

    @Test
    void creatorTypeIdFirstAndLast() throws Exception
    {
        for (String json : new String[] {
                "{'type':'group','value':{'children':["
                    + "{'type':'leaf','value':{'data':[1]}},"
                    + "{'type':'group','value':{'children':[],'label':'in'}}],'label':'top'}}",
                "{'value':{'children':["
                    + "{'value':{'data':[1]},'type':'leaf'},"
                    + "{'value':{'children':[],'label':'in'},'type':'group'}],'label':'top'},'type':'group'}"
        }) {
            CreatorHolder root = MAPPER.readValue(a2q(json), CreatorHolder.class);
            assertEquals("group", root.type);
            CreatorGroup top = (CreatorGroup) root.value;
            assertEquals("top", top.label);
            assertEquals(2, top.children.size());
            assertEquals("leaf", top.children.get(0).type);
            assertArrayEquals(new int[] { 1 }, ((Leaf) top.children.get(0).value).data);
            assertEquals("group", top.children.get(1).type);
            assertEquals("in", ((CreatorGroup) top.children.get(1).value).label);
        }
    }

    @Test
    void typeIdAlsoPropertyLast() throws Exception
    {
        VisibleHolder root = MAPPER.readValue(a2q(
                "{'value':{'children':[{'value':{'data':[5]},'type':'leaf'}]},'type':'group'}"),
                VisibleHolder.class);
        assertEquals("group", root.type);
        VisibleHolder child = ((VisibleGroup) root.value).children.get(0);
        assertEquals("leaf", child.type);
        assertArrayEquals(new int[] { 5 }, ((Leaf) child.value).data);
    }
}

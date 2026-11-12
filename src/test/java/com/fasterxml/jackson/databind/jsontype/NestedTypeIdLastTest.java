package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for nested polymorphic values (with {@code As.PROPERTY} inclusion)
 * where type id is not the first property, so that the outer value
 * gets buffered and inner values are read from buffered content.
 */
public class NestedTypeIdLastTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = Group.class, name = "group"),
        @JsonSubTypes.Type(value = Leaf.class, name = "leaf") })
    static abstract class Node { }

    static class Group extends Node {
        public String name;
        public List<Node> children;
    }

    static class Leaf extends Node {
        public int value;
    }

    // Visible type id, with default impl
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = "kind", visible = true, defaultImpl = VGroup.class)
    @JsonSubTypes({ @JsonSubTypes.Type(value = VGroup.class, name = "group"),
        @JsonSubTypes.Type(value = VLeaf.class, name = "leaf") })
    static abstract class VNode {
        public String kind;
    }

    static class VGroup extends VNode {
        public List<VNode> children;
    }

    static class VLeaf extends VNode {
        public int value;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void typeIdLastAtEveryLevel() throws Exception
    {
        Node n = MAPPER.readValue(
                a2q("{'name':'root','children':[{'name':'g1','children':[{'value':1,"
                + "'type':'leaf'},{'type':'leaf','value':2}],'type':'group'},"
                + "{'value':3,'type':'leaf'},{'children':[],'type':'group',"
                + "'name':'g2'}],'type':'group'}"), Node.class);
        Group root = (Group) n;
        assertEquals("root", root.name);
        assertEquals(3, root.children.size());
        Group g1 = (Group) root.children.get(0);
        assertEquals("g1", g1.name);
        assertEquals(1, ((Leaf) g1.children.get(0)).value);
        assertEquals(2, ((Leaf) g1.children.get(1)).value);
        assertEquals(3, ((Leaf) root.children.get(1)).value);
        Group g2 = (Group) root.children.get(2);
        assertEquals("g2", g2.name);
        assertEquals(0, g2.children.size());
    }

    @Test
    void deepNestingTypeIdLast() throws Exception
    {
        final int depth = 200;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{'children':[");
        }
        sb.append("{'value':42,'type':'leaf'}");
        for (int i = 0; i < depth; i++) {
            sb.append("],'name':'n").append(i).append("','type':'group'}");
        }
        Node n = MAPPER.readValue(a2q(sb.toString()), Node.class);
        for (int i = depth - 1; i >= 0; i--) {
            Group g = (Group) n;
            assertEquals("n" + i, g.name);
            assertEquals(1, g.children.size());
            n = g.children.get(0);
        }
        assertEquals(42, ((Leaf) n).value);
    }

    // Type id property with null value is ignored (and passed through) as before
    @Test
    void nullTypeIdBeforeRealOne() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        Node n = mapper.readValue(
                a2q("{'children':[{'type':null,'value':7,'type':'leaf'}],"
                + "'type':'group'}"), Node.class);
        assertEquals(7, ((Leaf) ((Group) n).children.get(0)).value);
    }

    @Test
    void visibleTypeIdLast() throws Exception
    {
        VNode n = MAPPER.readValue(
                a2q("{'children':[{'value':5,'kind':'leaf'},{'children':[],"
                + "'kind':'group'}],'kind':'group'}"), VNode.class);
        VGroup root = (VGroup) n;
        assertEquals("group", root.kind);
        VLeaf leaf = (VLeaf) root.children.get(0);
        assertEquals("leaf", leaf.kind);
        assertEquals(5, leaf.value);
        assertEquals("group", root.children.get(1).kind);
    }

    // Missing type id for nested value: default impl used
    @Test
    void missingTypeIdUsesDefaultImpl() throws Exception
    {
        VNode n = MAPPER.readValue(
                a2q("{'children':[{'children':[{'value':1,'kind':'leaf'}]}],"
                + "'kind':'group'}"), VNode.class);
        VGroup root = (VGroup) n;
        VGroup inner = (VGroup) root.children.get(0);
        assertNull(inner.kind);
        assertEquals(1, ((VLeaf) inner.children.get(0)).value);
    }

    // Missing type id for nested value, resolved to "no value" by problem handler:
    // must skip contents of the Object
    @Test
    void missingTypeIdResolvedToNull() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public JavaType handleMissingTypeId(DeserializationContext ctxt,
                            JavaType baseType, TypeIdResolver idResolver,
                            String failureMsg) throws IOException {
                        return ctxt.constructType(Void.class);
                    }
                })
                .build();
        Node n = mapper.readValue(
                a2q("{'children':[{'value':1,'children':[{'x':[1,2]}]},"
                + "{'value':2,'type':'leaf'}],'type':'group'}"), Node.class);
        Group root = (Group) n;
        assertEquals(2, root.children.size());
        assertNull(root.children.get(0));
        assertEquals(2, ((Leaf) root.children.get(1)).value);
    }

    @Test
    void caseInsensitiveTypeIdLast() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        Node n = mapper.readValue(
                a2q("{'children':[{'value':3,'TYPE':'leaf'}],'Type':'group'}"), Node.class);
        assertEquals(3, ((Leaf) ((Group) n).children.get(0)).value);
    }

    // Non-String type id (not handled by lookahead, uses regular handling)
    @Test
    void nonStringTypeIdLast() throws Exception
    {
        // "true" is not a known type id
        assertThrows(InvalidTypeIdException.class, () -> MAPPER.readValue(
                a2q("{'children':[{'value':3,'type':true}],'type':'group'}"), Node.class));
    }

    // Type id property must be removed from content seen by value deserializer
    @Test
    void typeIdNotExposedToValueDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        Node n = mapper.readValue(
                a2q("{'children':[{'value':3,'type':'leaf'}],'type':'group'}"), Node.class);
        assertEquals(3, ((Leaf) ((Group) n).children.get(0)).value);
        // but a second type id property is not removed
        assertThrows(UnrecognizedPropertyException.class, () -> mapper.readValue(
                a2q("{'children':[{'value':3,'type':'leaf','type':'leaf'}],"
                + "'type':'group'}"), Node.class));
    }
}

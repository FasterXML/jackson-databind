package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.JsonNode.OverwriteMode;

// for [databuind#1980] implementation
public class WithPathTest extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = sharedMapper();

    public void testValidWithObjectTrivial() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode match = root.withObject(JsonPointer.empty());
        assertSame(root, match);
    }

    public void testValidWithObjectSimpleExisting() throws Exception
    {
        final String DOC_STR = a2q("{'a':{'b':42,'c':{'x':1}}}");
        JsonNode doc = MAPPER.readTree(DOC_STR);
        ObjectNode match = doc.withObject(JsonPointer.compile("/a"));
        assertNotNull(match);
        assertTrue(match.isObject());
        assertEquals(a2q("{'b':42,'c':{'x':1}}"), match.toString());
        // should not modify the doc
        assertEquals(DOC_STR, doc.toString());

        match = doc.withObject(JsonPointer.compile("/a/c"));
        assertNotNull(match);
        assertEquals(a2q("{'x':1}"), match.toString());
        // should not modify the doc
        assertEquals(DOC_STR, doc.toString());
    }

    public void testValidWithObjectSimpleCreate() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode match = root.withObject(JsonPointer.compile("/a/b"));
        assertTrue(match.isObject());
        assertEquals(a2q("{}"), match.toString());
        match.put("value", 42);

        assertEquals(a2q("{'a':{'b':{'value':42}}}"),
                root.toString());

        // and with that
        ObjectNode match2 = root.withObject(JsonPointer.compile("/a/b"));
        assertSame(match, match2);
        match.put("value2", true);

        assertEquals(a2q("{'a':{'b':{'value':42,'value2':true}}}"),
                root.toString());
    }

    public void testValidWithObjectSimpleModify() throws Exception
    {
        final String DOC_STR = a2q("{'a':{'b':42}}");
        JsonNode doc = MAPPER.readTree(DOC_STR);
        ObjectNode  match = doc.withObject(JsonPointer.compile("/a/d"));
        assertNotNull(match);
        assertEquals("{}", match.toString());
        assertEquals(a2q("{'a':{'b':42,'d':{}}}"), doc.toString());
    }

    public void testObjectPathWithReplace() throws Exception
    {
        final JsonPointer abPath = JsonPointer.compile("/a/b");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 13);

        // First, without replacement (default) get exception
        try {
            root.withObject(abPath);
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "(mode `OverwriteMode.NULLS`)");
        }

        // Except fine via nulls (by default)
        root.putNull("a");
        root.withObject(abPath).put("value", 42);
        assertEquals(a2q("{'a':{'b':{'value':42}}}"),
                root.toString());

        // but not if prevented
        root = (ObjectNode) MAPPER.readTree(a2q("{'a':null}"));
        try {
            root.withObject(abPath, OverwriteMode.NONE, true);
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "(mode `OverwriteMode.NONE`)");
        }
    }

    public void testValidWithObjectWithArray() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putArray("arr");
        ObjectNode match = root.withObject(JsonPointer.compile("/arr/2"));
        assertTrue(match.isObject());
        match.put("value", 42);
        assertEquals(a2q("{'arr':[null,null,{'value':42}]}"),
                root.toString());

        // But also verify we can match
        ObjectNode match2 = root.withObject(JsonPointer.compile("/arr/2"));
        assertSame(match, match2);
        match.put("value2", true);
        assertEquals(a2q("{'arr':[null,null,{'value':42,'value2':true}]}"),
                root.toString());

        // And even more! `null`s can be replaced
    }
}

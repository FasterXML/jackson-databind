package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.*;

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

    public void testValidWithObjectSimple() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode match = root.withObject(JsonPointer.compile("/a/b"));
        assertTrue(match.isObject());
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
    }
}

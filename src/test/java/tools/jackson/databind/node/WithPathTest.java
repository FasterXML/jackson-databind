package tools.jackson.databind.node;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.JsonNodeException;
import tools.jackson.databind.node.ObjectNode;

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

    public void testObjectPathWithReplace() throws Exception
    {
        final JsonPointer abPath = JsonPointer.compile("/a/b");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 13);

        // First, without replacement (default) get exception
        try {
            root.withObject(abPath);
            fail("Should not pass");
        } catch (JsonNodeException e) {
            verifyException(e, "cannot traverse non-container");
        }

        // Except fine via nulls (by default)
        /*
        root.putNull("a");
        root.withObject(abPath).put("value", 42);
        assertEquals(a2q("{'a':{'b':{'value':42}}}"),
                root.toString());

        // but not if prevented
        root = (ObjectNode) MAPPER.readTree(a2q("{'a':null}"));
        try {
            root.withObject(abPath);
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "cannot traverse non-container");
        }
        */
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

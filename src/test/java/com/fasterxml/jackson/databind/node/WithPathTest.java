package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.JsonNode.OverwriteMode;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// for [databind#1980] implementation
public class WithPathTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();

    /*
    /**********************************************************************
    /* Test methods, withObject()
    /**********************************************************************
     */

    @Test
    public void testValidWithObjectTrivial() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode match = root.withObject(JsonPointer.empty());
        assertSame(root, match);
    }

    @Test
    public void testValidWithObjectSimpleExisting() throws Exception
    {
        _testValidWithObjectSimpleExisting(true);
        _testValidWithObjectSimpleExisting(false);
    }

    @Test
    public void testInvalidWithObjectTrivial() throws Exception
    {
        ArrayNode root = MAPPER.createArrayNode();
        try {
            root.withObject(JsonPointer.compile("/a"));
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace context node");
            verifyException(e, "ArrayNode");
        }
    }

    private void _testValidWithObjectSimpleExisting(boolean compile) throws Exception
    {
        final String DOC_STR = a2q("{'a':{'b':42,'c':{'x':1}}}");
        JsonNode doc = MAPPER.readTree(DOC_STR);
        ObjectNode match = compile
                ? doc.withObject(JsonPointer.compile("/a"))
                : doc.withObject("/a");
        assertNotNull(match);
        assertTrue(match.isObject());
        assertEquals(a2q("{'b':42,'c':{'x':1}}"), match.toString());
        // should not modify the doc
        assertEquals(DOC_STR, doc.toString());

        match = compile
                ? doc.withObject(JsonPointer.compile("/a/c"))
                : doc.withObject("/a/c");
        assertNotNull(match);
        assertEquals(a2q("{'x':1}"), match.toString());
        // should not modify the doc
        assertEquals(DOC_STR, doc.toString());
    }

    @Test
    public void testValidWithObjectSimpleCreate() throws Exception {
        _testValidWithObjectSimpleCreate(true);
        _testValidWithObjectSimpleCreate(false);
    }

    private void _testValidWithObjectSimpleCreate(boolean compile) throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode match = compile
                ? root.withObject(JsonPointer.compile("/a/b"))
                : root.withObject("/a/b");
        assertTrue(match.isObject());
        assertEquals(a2q("{}"), match.toString());
        match.put("value", 42);

        assertEquals(a2q("{'a':{'b':{'value':42}}}"),
                root.toString());

        // and with that
        ObjectNode match2 = compile
                ? root.withObject(JsonPointer.compile("/a/b"))
                : root.withObject("/a/b");
        assertSame(match, match2);
        match.put("value2", true);

        assertEquals(a2q("{'a':{'b':{'value':42,'value2':true}}}"),
                root.toString());
    }

    @Test
    public void testValidWithObjectSimpleModify() throws Exception {
        _testValidWithObjectSimpleModify(true);
        _testValidWithObjectSimpleModify(false);
    }

    private void _testValidWithObjectSimpleModify(boolean compile) throws Exception
    {
        final String DOC_STR = a2q("{'a':{'b':42}}");
        JsonNode doc = MAPPER.readTree(DOC_STR);
        ObjectNode  match = compile
                ? doc.withObject(JsonPointer.compile("/a/d"))
                : doc.withObject("/a/d");
        assertNotNull(match);
        assertEquals("{}", match.toString());
        assertEquals(a2q("{'a':{'b':42,'d':{}}}"), doc.toString());
    }

    @Test
    public void testObjectPathWithReplace() throws Exception {
        _testObjectPathWithReplace(true);
        _testObjectPathWithReplace(false);
    }

    private void _testObjectPathWithReplace(boolean compile) throws Exception
    {
        final JsonPointer abPath = JsonPointer.compile("/a/b");
        ObjectNode root = MAPPER.createObjectNode();
        root.put("a", 13);

        // First, without replacement (default) get exception
        _verifyObjectReplaceFail(root, abPath, null);

        // Except fine via nulls (by default)
        root.putNull("a");
        if (compile) {
             root.withObject(abPath).put("value", 42);
        } else {
             root.withObject("/a/b").put("value", 42);
        }
        assertEquals(a2q("{'a':{'b':{'value':42}}}"),
                root.toString());

        // but not if prevented
        root = (ObjectNode) MAPPER.readTree(a2q("{'a':null}"));
        _verifyObjectReplaceFail(root, abPath, OverwriteMode.NONE);
    }

    @Test
    public void testValidWithObjectWithArray() throws Exception {
        _testValidWithObjectWithArray(true);
        _testValidWithObjectWithArray(false);
    }

    private void _testValidWithObjectWithArray(boolean compile) throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putArray("arr");
        ObjectNode match = compile
                ? root.withObject(JsonPointer.compile("/arr/2"))
                : root.withObject("/arr/2");
        assertTrue(match.isObject());
        match.put("value", 42);
        assertEquals(a2q("{'arr':[null,null,{'value':42}]}"),
                root.toString());

        // But also verify we can match
        ObjectNode match2 = compile
                ? root.withObject(JsonPointer.compile("/arr/2"))
                : root.withObject("/arr/2");
        assertSame(match, match2);
        match2.put("value2", true);
        assertEquals(a2q("{'arr':[null,null,{'value':42,'value2':true}]}"),
                root.toString());

        // And even more! `null`s can be replaced by default
        ObjectNode match3 = compile
                ? root.withObject(JsonPointer.compile("/arr/0"))
                : root.withObject("/arr/0");
        assertEquals("{}", match3.toString());
        match3.put("value", "bar");
        assertEquals(a2q("{'arr':[{'value':'bar'},null,{'value':42,'value2':true}]}"),
                root.toString());

        // But not if prevented
        _verifyObjectReplaceFail(root, "/arr/1", OverwriteMode.NONE);
    }

    private void _verifyObjectReplaceFail(JsonNode doc, String ptrExpr, OverwriteMode mode) {
        _verifyObjectReplaceFail(doc, JsonPointer.compile(ptrExpr), mode);
    }

    private void _verifyObjectReplaceFail(JsonNode doc, JsonPointer ptr, OverwriteMode mode) {
        try {
            if (mode == null) {
                // default is "NULLS":
                mode = OverwriteMode.NULLS;
                doc.withObject(ptr);
            } else {
                doc.withObject(ptr, mode, true);
            }
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "(mode `OverwriteMode."+mode.name()+"`)");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, withObjectProperty()/withObject(exprOrProperty)
    /**********************************************************************
     */

    // [databind#4095]
    @Test
    public void testWithObjectProperty() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();

        // First: create new property value
        ObjectNode match = root.withObjectProperty("a");
        assertTrue(match.isObject());
        assertEquals(a2q("{}"), match.toString());
        match.put("value", 42);
        assertEquals(a2q("{'a':{'value':42}}"), root.toString());

        // Second: match existing Object property
        ObjectNode match2 = root.withObjectProperty("a");
        assertSame(match, match2);
        match.put("value2", true);

        assertEquals(a2q("{'a':{'value':42,'value2':true}}"),
                root.toString());

        // Third: match and overwrite existing null node
        JsonNode root2 = MAPPER.readTree("{\"b\": null}");
        ObjectNode match3 = root2.withObjectProperty("b");
        assertNotSame(match, match3);
        assertEquals("{\"b\":{}}", root2.toString());

        // and then failing case
        JsonNode root3 = MAPPER.readTree("{\"c\": 123}");
        try {
            root3.withObjectProperty("c");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
        }
    }

    // [databind#4096]
    @Test
    public void testWithObjectAdnExprOrProp() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();

        // First: create new property value
        ObjectNode match = root.withObject("a");
        assertTrue(match.isObject());
        assertEquals(a2q("{}"), match.toString());
        match.put("value", 42);
        assertEquals(a2q("{'a':{'value':42}}"), root.toString());

        // and then with JsonPointer expr
        match = root.withObject("/a/b");
        assertTrue(match.isObject());
        assertEquals(a2q("{}"), match.toString());
        assertEquals(a2q("{'a':{'value':42,'b':{}}}"), root.toString());

        // Then existing prop:
        assertEquals(a2q("{'value':42,'b':{}}"),
                root.withObject("a").toString());
        assertEquals(a2q("{}"),
                root.withObject("/a/b").toString());

        // and then failing case
        JsonNode root3 = MAPPER.readTree("{\"c\": 123}");
        try {
            root3.withObject("c");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
        }
        try {
            root3.withObject("/c");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, withArray()
    /**********************************************************************
     */

    @Test
    public void testValidWithArrayTrivial() throws Exception
    {
        // First, empty path, existing Array
        ArrayNode root = MAPPER.createArrayNode();
        ArrayNode match = root.withArray(JsonPointer.empty());
        assertSame(root, match);

        // Then existing Object, empty Path -> fail
        ObjectNode rootOb = MAPPER.createObjectNode();
        try {
            rootOb.withArray(JsonPointer.empty());
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Can only call `withArray()` with empty");
            verifyException(e, "on `ArrayNode`");
        }
    }

    // From Javadoc example
    @Test
    public void testValidWithArraySimple() throws Exception {
        _testValidWithArraySimple(true);
        _testValidWithArraySimple(false);
    }

    @Test
    public void testInvalidWithArrayTrivial() throws Exception
    {
        ArrayNode root = MAPPER.createArrayNode();
        try {
            root.withArray(JsonPointer.compile("/a"));
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace context node");
            verifyException(e, "ArrayNode");
        }
    }

    private void _testValidWithArraySimple(boolean compile) throws Exception
    {
        final String DOC_STR = a2q(
                "{'a':{'b':[1,2],'c':true}}"
                );
        JsonNode doc = MAPPER.readTree(DOC_STR);

        {
            ArrayNode match = compile
                    ? doc.withArray(JsonPointer.compile("/a/b"))
                    : doc.withArray("/a/b");
            assertEquals(a2q("[1,2]"), match.toString());
            // should not modify the doc
            assertEquals(DOC_STR, doc.toString());
        }
        {
            ArrayNode match = compile
                    ? doc.withArray(JsonPointer.compile("/a/x"))
                    : doc.withArray("/a/x");
            assertEquals(a2q("[]"), match.toString());
            // does modify the doc
            assertEquals(a2q(
                    "{'a':{'b':[1,2],'c':true,'x':[]}}"), doc.toString());
        }
        // And then replacements: first, fail
        _verifyArrayReplaceFail(doc, "/a/b/0", null);

        // then acceptable replacement
        {
            ArrayNode match = compile
                    ? doc.withArray(JsonPointer.compile("/a/b/0"), OverwriteMode.ALL, true)
                    : doc.withArray("/a/b/0", OverwriteMode.ALL, true);
            assertEquals(a2q("[]"), match.toString());
            // does further modify the doc
            assertEquals(a2q(
                    "{'a':{'b':[[],2],'c':true,'x':[]}}"), doc.toString());
        }
    }

    private void _verifyArrayReplaceFail(JsonNode doc, String ptrExpr, OverwriteMode mode) {
        _verifyArrayReplaceFail(doc, JsonPointer.compile(ptrExpr), mode);
    }

    private void _verifyArrayReplaceFail(JsonNode doc, JsonPointer ptr, OverwriteMode mode) {
        try {
            if (mode == null) {
                // default is "NULLS":
                mode = OverwriteMode.NULLS;
                doc.withArray(ptr);
            } else {
                doc.withArray(ptr, mode, true);
            }
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
            verifyException(e, "(mode `OverwriteMode."+mode.name()+"`)");
        }
    }

    // [databind#3882]
    @Test
    public void testWithArray3882() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode aN = root.withArray("/key/0/a",
                JsonNode.OverwriteMode.ALL, true);
        aN.add(123);
        assertEquals(a2q("{'key':[{'a':[123]}]}"),
                root.toString());

        // And then the original case
        root = MAPPER.createObjectNode();
        aN = root.withArray(JsonPointer.compile("/key1/array1/0/element1"),
            JsonNode.OverwriteMode.ALL, true);
        aN.add("v1");
        assertEquals(a2q("{'key1':{'array1':[{'element1':['v1']}]}}"),
                root.toString());
    }

    /*
    /**********************************************************************
    /* Test methods, withArrayProperty()/withArray(exprOrProperty)
    /**********************************************************************
     */

    // [databind#4095]
    @Test
    public void testWithArrayProperty() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();

        // First: create new property value
        ArrayNode match = root.withArrayProperty("a");
        assertTrue(match.isArray());
        assertEquals(a2q("[]"), match.toString());
        match.add(42);
        assertEquals(a2q("{'a':[42]}"), root.toString());

        // Second: match existing Object property
        ArrayNode match2 = root.withArrayProperty("a");
        assertSame(match, match2);
        match.add(true);

        assertEquals(a2q("{'a':[42,true]}"), root.toString());

        // Third: match and overwrite existing null node
        JsonNode root2 = MAPPER.readTree("{\"b\": null}");
        ArrayNode match3 = root2.withArrayProperty("b");
        assertNotSame(match, match3);
        assertEquals("{\"b\":[]}", root2.toString());
        
        // and then failing case
        JsonNode root3 = MAPPER.readTree("{\"c\": 123}");
        try {
            root3.withArrayProperty("c");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
        }
    }

    // [databind#4096]
    @Test
    public void testWithArrayAndExprOrProp() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();

        // First: create new property value
        ArrayNode match = root.withArray("a");
        assertTrue(match.isArray());
        assertEquals(a2q("[]"), match.toString());
        match.add(42);
        assertEquals(a2q("{'a':[42]}"), root.toString());

        match = root.withArray("/b");
        assertEquals(a2q("{'a':[42],'b':[]}"), root.toString());

        // Second: match existing Object property
        assertEquals(a2q("[42]"), root.withArray("a").toString());
        assertEquals(a2q("[42]"), root.withArray("/a").toString());

        // and then failing case
        JsonNode root3 = MAPPER.readTree("{\"c\": 123}");
        try {
            root3.withArrayProperty("c");
            fail("Should not pass");
        } catch (UnsupportedOperationException e) {
            verifyException(e, "Cannot replace `JsonNode` of type ");
        }
    }
}

package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequiredAccessorTest
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final JsonNode TEST_OBJECT, TEST_ARRAY;

    public RequiredAccessorTest() throws Exception {
        TEST_OBJECT = MAPPER.readTree(a2q(
 "{ 'data' : { 'primary' : 15, 'vector' : [ 'yes', false ], 'nullable' : null  },\n"
+"  'array' : [ true,   {\"messsage\":'hello', 'value' : 42, 'misc' : [1, 2] }, null, 0.25 ]\n"
+"}"
        ));
        TEST_ARRAY = MAPPER.readTree(a2q(
 "[ true, { 'data' : { 'primary' : 15, 'vector' : [ 'yes', false ]  } }, 0.25, 'last' ]"
        ));
    }

    public void testIMPORTANT() {
        _checkRequiredAtFail(TEST_OBJECT, "/data/weird/and/more", "/weird/and/more");
    }

    public void testRequiredAtObjectOk() throws Exception {
        assertNotNull(TEST_OBJECT.requiredAt("/array"));
        assertNotNull(TEST_OBJECT.requiredAt("/array/0"));
        assertTrue(TEST_OBJECT.requiredAt("/array/0").isBoolean());
        assertNotNull(TEST_OBJECT.requiredAt("/array/1/misc/1"));
        assertEquals(2, TEST_OBJECT.requiredAt("/array/1/misc/1").intValue());
    }

    public void testRequiredAtArrayOk() throws Exception {
        assertTrue(TEST_ARRAY.requiredAt("/0").isBoolean());
        assertTrue(TEST_ARRAY.requiredAt("/1").isObject());
        assertNotNull(TEST_ARRAY.requiredAt("/1/data/primary"));
        assertNotNull(TEST_ARRAY.requiredAt("/1/data/vector/1"));
    }

    public void testRequiredAtFailOnObjectBasic() throws Exception {
        _checkRequiredAtFail(TEST_OBJECT, "/0", "/0");
        _checkRequiredAtFail(TEST_OBJECT, "/bogus", "/bogus");
        _checkRequiredAtFail(TEST_OBJECT, "/data/weird/and/more", "/weird/and/more");

        _checkRequiredAtFail(TEST_OBJECT, "/data/vector/other/3", "/other/3");

        _checkRequiredAtFail(TEST_OBJECT, "/data/primary/more", "/more");
    }

    public void testRequiredAtFailOnArray() throws Exception {
        _checkRequiredAtFail(TEST_ARRAY, "/1/data/vector/25", "/25");
        _checkRequiredAtFail(TEST_ARRAY, "/0/data/x", "/data/x");
    }

    private void _checkRequiredAtFail(JsonNode doc, String fullPath, String mismatchPart) {
        try {
            JsonNode n = doc.requiredAt(fullPath);
            fail("Should NOT pass: got node ("+n.getClass().getSimpleName()+") -> {"+n+"}");
        } catch (IllegalArgumentException e) {
            verifyException(e, "No node at '"+fullPath+"' (unmatched part: '"+mismatchPart+"')");
        }
    }

    public void testSimpleRequireOk() throws Exception {
        // first basic working accessors on node itself
        assertSame(TEST_OBJECT, TEST_OBJECT.require());
        assertSame(TEST_OBJECT, TEST_OBJECT.requireNonNull());
        assertSame(TEST_OBJECT, TEST_OBJECT.requiredAt(""));
        assertSame(TEST_OBJECT, TEST_OBJECT.requiredAt(JsonPointer.compile("")));

        assertSame(TEST_OBJECT.get("data"), TEST_OBJECT.required("data"));
        assertSame(TEST_ARRAY.get(0), TEST_ARRAY.required(0));
        assertSame(TEST_ARRAY.get(3), TEST_ARRAY.required(3));

        // check diff between missing, null nodes
        TEST_OBJECT.path("data").path("nullable").require();

        try {
            JsonNode n = TEST_OBJECT.path("data").path("nullable").requireNonNull();
            fail("Should not pass; got: "+n);
        } catch (IllegalArgumentException e) {
            verifyException(e, "requireNonNull() called on `NullNode`");
        }
    }

    public void testSimpleRequireFail() throws Exception {
        try {
            TEST_OBJECT.required("bogus");
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "No value for property 'bogus'");
        }

        try {
            TEST_ARRAY.required("bogus");
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Node of type `ArrayNode` has no fields");
        }
    }

    // [databind#3005]
    public void testRequiredAtFailOnObjectScalar3005() throws Exception
    {
        JsonNode n = MAPPER.readTree("{\"simple\":5}");
        try {
            JsonNode match = n.requiredAt("/simple/property");
            fail("Should NOT pass: got node ("+match.getClass().getSimpleName()+") -> {"+match+"}");
        } catch (IllegalArgumentException e) {
            verifyException(e, "No node at '/simple/property' (unmatched part: '/property')");
        }
    }
}

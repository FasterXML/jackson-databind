package com.fasterxml.jackson.databind.node;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * This unit test suite tries to verify that JsonNode-based trees
 * can be deserialized as expected.
 */
public class TestTreeDeserialization
    extends BaseMapTest
{
    final static class Bean {
        int _x;
        JsonNode _node;

        public void setX(int x) { _x = x; }
        public void setNode(JsonNode n) { _node = n; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * This test checks that is possible to mix "regular" Java objects
     * and JsonNode.
     */
    public void testMixed() throws IOException
    {
        ObjectMapper om = new ObjectMapper();
        String JSON = "{\"node\" : { \"a\" : 3 }, \"x\" : 9 }";
        Bean bean = om.readValue(JSON, Bean.class);

        assertEquals(9, bean._x);
        JsonNode n = bean._node;
        assertNotNull(n);
        assertEquals(1, n.size());
        ObjectNode on = (ObjectNode) n;
        assertEquals(3, on.get("a").intValue());
    }

    /// Verifying [JACKSON-143]
    public void testArrayNodeEquality()
    {
        ArrayNode n1 = new ArrayNode(null);
        ArrayNode n2 = new ArrayNode(null);

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));

        n1.add(TextNode.valueOf("Test"));

        assertFalse(n1.equals(n2));
        assertFalse(n2.equals(n1));

        n2.add(TextNode.valueOf("Test"));

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
    }

    public void testObjectNodeEquality()
    {
        ObjectNode n1 = new ObjectNode(null);
        ObjectNode n2 = new ObjectNode(null);

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));

        n1.set("x", TextNode.valueOf("Test"));

        assertFalse(n1.equals(n2));
        assertFalse(n2.equals(n1));

        n2.set("x", TextNode.valueOf("Test"));

        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
    }

    public void testReadFromString() throws Exception
    {
        String json = "{\"field\":\"{\\\"name\\\":\\\"John Smith\\\"}\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jNode = mapper.readValue(json, JsonNode.class);

        String generated = mapper.writeValueAsString( jNode);  //back slashes are gone
        JsonNode out = mapper.readValue( generated, JsonNode.class );   //crashes here
        assertTrue(out.isObject());
        assertEquals(1, out.size());
        String value = out.path("field").asText();
        assertNotNull(value);
    }

    // Issue#186
    public void testNullHandling() throws Exception
    {
        // First, a stand-alone null
        JsonNode n = objectReader().readTree("null");
        assertNotNull(n);
        assertTrue(n.isNull());

        n = objectMapper().readTree("null");
        assertNotNull(n);
        assertTrue(n.isNull());
        
        // Then object property
        ObjectNode root = (ObjectNode) objectReader().readTree("{\"x\":null}");
        assertEquals(1, root.size());
        n = root.get("x");
        assertNotNull(n);
        assertTrue(n.isNull());
    }

    final static class CovarianceBean {
        ObjectNode _object;
        ArrayNode _array;

        public void setObject(ObjectNode n) { _object = n; }
        public void setArray(ArrayNode n) { _array = n; }
    }

    public void testNullHandlingCovariance() throws Exception
    {
        String JSON = "{\"object\" : null, \"array\" : null }";
        CovarianceBean bean = objectMapper().readValue(JSON, CovarianceBean.class);

        ObjectNode on = bean._object;
        assertNull(on);

        ArrayNode an = bean._array;
        assertNull(an);
    }
}

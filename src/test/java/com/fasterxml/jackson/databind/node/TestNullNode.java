package com.fasterxml.jackson.databind.node;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestNullNode extends NodeTestBase
{
    final static class CovarianceBean {
        ObjectNode _object;
        ArrayNode _array;

        public void setObject(ObjectNode n) { _object = n; }
        public void setArray(ArrayNode n) { _array = n; }
    }

    @SuppressWarnings("serial")
    static class MyNull extends NullNode { }

    private final ObjectMapper MAPPER = sharedMapper();

    public void testBasicsWithNullNode() throws Exception
    {
        // Let's use something that doesn't add much beyond JsonNode base
        NullNode n = NullNode.instance;

        // basic properties
        assertFalse(n.isContainerNode());
        assertFalse(n.isBigDecimal());
        assertFalse(n.isBigInteger());
        assertFalse(n.isBinary());
        assertFalse(n.isBoolean());
        assertFalse(n.isPojo());
        assertFalse(n.isMissingNode());

        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        // fallback accessors
        assertFalse(n.booleanValue());
        assertNull(n.numberValue());
        assertEquals(0, n.intValue());
        assertEquals(0L, n.longValue());
        assertEquals(BigDecimal.ZERO, n.decimalValue());
        assertEquals(BigInteger.ZERO, n.bigIntegerValue());

        assertEquals(0, n.size());
        assertTrue(n.isEmpty());
        assertFalse(n.elements().hasNext());
        assertFalse(n.fieldNames().hasNext());
        // path is never null; but does point to missing node
        assertNotNull(n.path("xyz"));
        assertTrue(n.path("xyz").isMissingNode());

        assertFalse(n.has("field"));
        assertFalse(n.has(3));

        assertNodeNumbersForNonNumeric(n);

        // 2.4
        assertEquals("foo", n.asText("foo"));
    }

    public void testNullHandling() throws Exception
    {
        // First, a stand-alone null
        JsonNode n = MAPPER.readTree("null");
        assertNotNull(n);
        assertTrue(n.isNull());
        assertFalse(n.isNumber());
        assertFalse(n.isTextual());
        assertEquals("null", n.asText());
        assertEquals(n, NullNode.instance);

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

    public void testNullSerialization() throws Exception
    {
        StringWriter sw = new StringWriter();
        MAPPER.writeValue(sw, NullNode.instance);
        assertEquals("null", sw.toString());
    }

    public void testNullHandlingCovariance() throws Exception
    {
        String JSON = "{\"object\" : null, \"array\" : null }";
        CovarianceBean bean = MAPPER.readValue(JSON, CovarianceBean.class);

        ObjectNode on = bean._object;
        assertNull(on);

        ArrayNode an = bean._array;
        assertNull(an);
    }

    @SuppressWarnings("unlikely-arg-type")
    public void testNullEquality() throws Exception
    {
        JsonNode n = MAPPER.nullNode();
        assertTrue(n.isNull());
        assertEquals(n, new MyNull());
        assertEquals(new MyNull(), n);

        assertFalse(n.equals(null));
        assertFalse(n.equals("foo"));
    }
}

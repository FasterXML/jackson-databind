package com.fasterxml.jackson.databind.node;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.util.RawValue;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

/**
 * Additional tests for {@link ArrayNode} container class.
 */
public class ArrayNodeTest
    extends BaseMapTest
{
    public void testDirectCreation() throws IOException
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);

        assertFalse(n.isBoolean());
        assertFalse(n.isTextual());
        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        assertStandardEquals(n);
        assertFalse(n.elements().hasNext());
        assertFalse(n.fieldNames().hasNext());
        assertTrue(n.isEmpty());
        TextNode text = TextNode.valueOf("x");
        n.add(text);
        assertEquals(1, n.size());
        assertFalse(n.isEmpty());
        assertFalse(0 == n.hashCode());
        assertTrue(n.elements().hasNext());
        // no field names for arrays
        assertFalse(n.fieldNames().hasNext());
        assertNull(n.get("x")); // not used with arrays
        assertTrue(n.path("x").isMissingNode());
        assertSame(text, n.get(0));

        // single element, so:
        assertFalse(n.has("field"));
        assertFalse(n.hasNonNull("field"));
        assertTrue(n.has(0));
        assertTrue(n.hasNonNull(0));
        assertFalse(n.has(1));
        assertFalse(n.hasNonNull(1));

        // add null node too
        n.add((JsonNode) null);
        assertEquals(2, n.size());
        assertTrue(n.get(1).isNull());
        assertTrue(n.has(1));
        assertFalse(n.hasNonNull(1));
        // change to text
        n.set(1, text);
        assertSame(text, n.get(1));
        n.set(0, (JsonNode) null);
        assertTrue(n.get(0).isNull());

        // and finally, clear it all
        ArrayNode n2 = new ArrayNode(JsonNodeFactory.instance);
        n2.add("foobar");
        assertFalse(n.equals(n2));
        n.addAll(n2);
        assertEquals(3, n.size());

        assertFalse(n.get(0).isTextual());
        assertNotNull(n.remove(0));
        assertEquals(2, n.size());
        assertTrue(n.get(0).isTextual());
        assertNull(n.remove(-1));
        assertNull(n.remove(100));
        assertEquals(2, n.size());

        ArrayList<JsonNode> nodes = new ArrayList<JsonNode>();
        nodes.add(text);
        n.addAll(nodes);
        assertEquals(3, n.size());
        assertNull(n.get(10000));
        assertNull(n.remove(-4));

        TextNode text2 = TextNode.valueOf("b");
        n.insert(0, text2);
        assertEquals(4, n.size());
        assertSame(text2, n.get(0));

        assertNotNull(n.addArray());
        assertEquals(5, n.size());
        n.addPOJO("foo");
        assertEquals(6, n.size());

        n.removeAll();
        assertEquals(0, n.size());
    }

    public void testDirectCreation2() throws IOException
    {
        JsonNodeFactory f = objectMapper().getNodeFactory();
        ArrayList<JsonNode> list = new ArrayList<>();
        list.add(f.booleanNode(true));
        list.add(f.textNode("foo"));
        ArrayNode n = new ArrayNode(f, list);
        assertEquals(2, n.size());
        assertTrue(n.get(0).isBoolean());
        assertTrue(n.get(1).isTextual());

        // also, should fail with invalid set attempt
        try {
            n.set(2, f.nullNode());
            fail("Should not pass");
        } catch (IndexOutOfBoundsException e) {
            verifyException(e, "illegal index");
        }
        n.insert(1, (String) null);
        assertEquals(3, n.size());
        assertTrue(n.get(0).isBoolean());
        assertTrue(n.get(1).isNull());
        assertTrue(n.get(2).isTextual());

        n.removeAll();
        n.insert(0, (JsonNode) null);
        assertEquals(1, n.size());
        assertTrue(n.get(0).isNull());
    }

    public void testArraySet() throws IOException {
        final ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < 20; i++) {
            array.add("Original Data");
        }

        array.setPOJO(0, "MyPojo");
        array.setRawValue(1, new RawValue("MyRawValue"));
        array.setNull(2);
        array.set(3, (short) 155);
        array.set(4, Short.valueOf((short) 130));
        array.set(5, 132);
        array.set(6, Integer.valueOf(452));
        array.set(7, 4342L);
        array.set(8, Long.valueOf(154242L));
        array.set(9, 1.22f);
        array.set(10, Float.valueOf(242.1f));
        array.set(11, 132.212D);
        array.set(12, Double.valueOf(231.3D));
        array.set(13, BigDecimal.TEN);
        array.set(14, BigInteger.ONE);
        array.set(15, "Modified Data");
        array.set(16, true);
        array.set(17, Boolean.FALSE);
        array.set(18, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});

        assertEquals("MyPojo", ((POJONode) array.get(0)).getPojo());
        assertEquals(new RawValue("MyRawValue"), ((POJONode) array.get(1)).getPojo());
        assertEquals(NullNode.instance, array.get(2));
        assertEquals((short) 155, array.get(3).shortValue());
        assertEquals((short) 130, array.get(4).shortValue());
        assertEquals(132, array.get(5).intValue());
        assertEquals(452, array.get(6).intValue());
        assertEquals(4342L, array.get(7).longValue());
        assertEquals(154242L, array.get(8).longValue());
        assertEquals(1.22f, array.get(9).floatValue());
        assertEquals(242.1f, array.get(10).floatValue());
        assertEquals(132.212D, array.get(11).doubleValue());
        assertEquals(231.3D, array.get(12).doubleValue());
        assertEquals(0, BigDecimal.TEN.compareTo(array.get(13).decimalValue()));
        assertEquals(BigInteger.ONE, array.get(14).bigIntegerValue());
        assertEquals("Modified Data", array.get(15).textValue());
        assertTrue(array.get(16).booleanValue());
        assertFalse(array.get(17).booleanValue());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, array.get(18).binaryValue());

        assertEquals(20, array.size());
        for (int i = 0; i < 20; i++) {
            if (i <= 18) {
                assertNotEquals("Original Data", array.get(i).textValue());
            } else {
                assertEquals("Original Data", array.get(i).textValue());
            }
        }
    }

    public void testArrayViaMapper() throws Exception
    {
        final String JSON = "[[[-0.027512,51.503221],[-0.008497,51.503221],[-0.008497,51.509744],[-0.027512,51.509744]]]";

        JsonNode n = objectMapper().readTree(JSON);
        assertNotNull(n);
        assertTrue(n.isArray());
        ArrayNode an = (ArrayNode) n;
        assertEquals(1, an.size());
        ArrayNode an2 = (ArrayNode) n.get(0);
        assertTrue(an2.isArray());
        assertEquals(4, an2.size());
    }

    public void testAdds()
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        assertNotNull(n.addArray());
        assertNotNull(n.addObject());
        n.addPOJO("foobar");
        n.add(1);
        n.add(1L);
        n.add(0.5);
        n.add(0.5f);
        n.add(new BigDecimal("0.2"));
        n.add(BigInteger.TEN);
        assertEquals(9, n.size());
        assertFalse(n.isEmpty());

        assertNotNull(n.insertArray(0));
        assertNotNull(n.insertObject(0));
        n.insertPOJO(2, "xxx");
        assertEquals(12, n.size());

        n.insert(0, BigInteger.ONE);
        n.insert(0, new BigDecimal("0.1"));
        assertEquals(14, n.size());
    }

    public void testNullAdds()
    {
        JsonNodeFactory f = objectMapper().getNodeFactory();
        ArrayNode array = f.arrayNode(14);

        array.add((BigDecimal) null);
        array.add((BigInteger) null);
        array.add((Boolean) null);
        array.add((byte[]) null);
        array.add((Double) null);
        array.add((Float) null);
        array.add((Integer) null);
        array.add((JsonNode) null);
        array.add((Long) null);
        array.add((String) null);

        assertEquals(10, array.size());

        for (JsonNode node : array) {
            assertTrue(node.isNull());
        }
    }

    public void testAddAllWithNullInCollection()
    {
        // preparation
        final ArrayNode array = JsonNodeFactory.instance.arrayNode();

        // test
        array.addAll(asList(null, JsonNodeFactory.instance.objectNode()));

        // assertions
        assertEquals(2, array.size());

        for (JsonNode node : array) {
            assertNotNull(node);
        }
        assertEquals(NullNode.getInstance(), array.get(0));
    }

    public void testNullInserts()
    {
        JsonNodeFactory f = objectMapper().getNodeFactory();
        ArrayNode array = f.arrayNode(3);

        array.insert(0, (BigDecimal) null);
        array.insert(0, (BigInteger) null);
        array.insert(0, (Boolean) null);
        // Offsets out of the range are fine; negative become 0;
        // super big just add at the end
        array.insert(-56, (byte[]) null);
        array.insert(0, (Double) null);
        array.insert(200, (Float) null);
        array.insert(0, (Integer) null);
        array.insert(1, (JsonNode) null);
        array.insert(array.size(), (Long) null);
        array.insert(1, (String) null);

        assertEquals(10, array.size());

        for (JsonNode node : array) {
            assertTrue(node.isNull());
        }
    }

    public void testNullSet()
    {
        JsonNodeFactory f = objectMapper().getNodeFactory();
        ArrayNode array = f.arrayNode(3);

        for (int i = 0; i < 14; i++) {
            array.add("Not Null");
        }

        for (JsonNode node : array) {
            assertFalse(node.isNull());
        }

        array.set(0, (BigDecimal) null);
        array.set(1, (BigInteger) null);
        array.set(2, (Boolean) null);
        array.set(3, (byte[]) null);
        array.set(4, (Double) null);
        array.set(5, (Float) null);
        array.set(6, (Integer) null);
        array.set(7, (Short) null);
        array.set(8, (JsonNode) null);
        array.set(9, (Long) null);
        array.set(10, (String) null);
        array.setNull(11);
        array.setRawValue(12, null);
        array.setPOJO(13, null);

        assertEquals(14, array.size());

        for (JsonNode node : array) {
            assertTrue(node.isNull());
        }
    }

    public void testNullChecking()
    {
        ArrayNode a1 = JsonNodeFactory.instance.arrayNode();
        ArrayNode a2 = JsonNodeFactory.instance.arrayNode();
        // used to throw NPE before fix:
        a1.addAll(a2);
        assertEquals(0, a1.size());
        assertEquals(0, a2.size());

        a2.addAll(a1);
        assertEquals(0, a1.size());
        assertEquals(0, a2.size());
    }

    public void testNullChecking2()
    {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode src = mapper.createArrayNode();
        ArrayNode dest = mapper.createArrayNode();
        src.add("element");
        dest.addAll(src);
    }

    public void testParser() throws Exception
    {
        ArrayNode n = new ArrayNode(JsonNodeFactory.instance);
        n.add(123);
        TreeTraversingParser p = new TreeTraversingParser(n, null);
        p.setCodec(null);
        assertNull(p.getCodec());
        assertNotNull(p.getParsingContext());
        assertTrue(p.getParsingContext().inRoot());
        assertNotNull(p.getTokenLocation());
        assertNotNull(p.getCurrentLocation());
        assertNull(p.getEmbeddedObject());
        assertNull(p.currentNode());

        //assertNull(p.getNumberType());

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertNotNull(p.getParsingContext());
        assertTrue(p.getParsingContext().inArray());
        p.skipChildren();
        assertToken(JsonToken.END_ARRAY, p.currentToken());
        p.close();

        p = new TreeTraversingParser(n, null);
        p.nextToken();
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        p.close();
    }

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

    public void testSimpleArray() throws Exception
    {
        ArrayNode result = objectMapper().createArrayNode();

        assertTrue(result.isArray());
        assertType(result, ArrayNode.class);

        assertFalse(result.isObject());
        assertFalse(result.isNumber());
        assertFalse(result.isNull());
        assertFalse(result.isTextual());

        // and let's add stuff...
        result.add(false);
        result.insertNull(0);

        // should be equal to itself no matter what
        assertEquals(result, result);
        assertFalse(result.equals(null)); // but not to null

        // plus see that we can access stuff
        assertEquals(NullNode.instance, result.path(0));
        assertEquals(NullNode.instance, result.get(0));
        assertEquals(BooleanNode.FALSE, result.path(1));
        assertEquals(BooleanNode.FALSE, result.get(1));
        assertEquals(2, result.size());

        assertNull(result.get(-1));
        assertNull(result.get(2));
        JsonNode missing = result.path(2);
        assertTrue(missing.isMissingNode());
        assertTrue(result.path(-100).isMissingNode());

        // then construct and compare
        ArrayNode array2 = objectMapper().createArrayNode();
        array2.addNull();
        array2.add(false);
        assertEquals(result, array2);

        // plus remove entries
        JsonNode rm1 = array2.remove(0);
        assertEquals(NullNode.instance, rm1);
        assertEquals(1, array2.size());
        assertEquals(BooleanNode.FALSE, array2.get(0));
        assertFalse(result.equals(array2));

        JsonNode rm2 = array2.remove(0);
        assertEquals(BooleanNode.FALSE, rm2);
        assertEquals(0, array2.size());
    }

    public void testSimpleMismatch() throws Exception
    {
        ObjectMapper mapper = objectMapper();
        try {
            mapper.readValue(" 123 ", ArrayNode.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "from Integer value (token `JsonToken.VALUE_NUMBER_INT`)");
        }
    }
}

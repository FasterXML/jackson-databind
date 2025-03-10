package com.fasterxml.jackson.databind.node;

import java.util.Comparator;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;
import com.fasterxml.jackson.databind.util.RawValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for {@link JsonNode} base class and some features
 * of implementation classes
 */
public class JsonNodeBasicTest extends NodeTestBase
{
    private final ObjectMapper MAPPER = objectMapper();

    @Test
    public void testBoolean() throws Exception
    {
        BooleanNode f = BooleanNode.getFalse();
        assertNotNull(f);
        assertTrue(f.isBoolean());
        assertSame(f, BooleanNode.valueOf(false));
        assertStandardEquals(f);
        assertFalse(f.booleanValue());
        assertFalse(f.asBoolean());
        assertEquals("false", f.asText());
        assertEquals(JsonToken.VALUE_FALSE, f.asToken());

        assertFalse(f.isNumber());
        assertFalse(f.canConvertToInt());
        assertFalse(f.canConvertToLong());
        assertFalse(f.canConvertToExactIntegral());

        // and ditto for true
        BooleanNode t = BooleanNode.getTrue();
        assertNotNull(t);
        assertTrue(t.isBoolean());
        assertSame(t, BooleanNode.valueOf(true));
        assertStandardEquals(t);
        assertTrue(t.booleanValue());
        assertTrue(t.asBoolean());
        assertEquals("true", t.asText());
        assertEquals(JsonToken.VALUE_TRUE, t.asToken());

        assertNodeNumbers(f, 0, 0.0);
        assertNodeNumbers(t, 1, 1.0);

        JsonNode result = objectMapper().readTree("true\n");
        assertFalse(result.isNull());
        assertFalse(result.isNumber());
        assertFalse(result.isTextual());
        assertTrue(result.isBoolean());
        assertType(result, BooleanNode.class);
        assertTrue(result.booleanValue());
        assertEquals("true", result.asText());
        assertFalse(result.isMissingNode());

        // also, equality should work ok
        assertEquals(result, BooleanNode.valueOf(true));
        assertEquals(result, BooleanNode.getTrue());

        assertNonContainerStreamMethods(f);
    }

    @Test
    public void testBinary() throws Exception
    {
        assertNull(BinaryNode.valueOf(null));
        assertNull(BinaryNode.valueOf(null, 0, 0));

        BinaryNode empty = BinaryNode.valueOf(new byte[1], 0, 0);
        assertSame(BinaryNode.EMPTY_BINARY_NODE, empty);
        assertStandardEquals(empty);

        byte[] data = new byte[3];
        data[1] = (byte) 3;
        BinaryNode n = BinaryNode.valueOf(data, 1, 1);
        assertFalse(n.isNumber());
        assertFalse(n.canConvertToInt());
        assertFalse(n.canConvertToLong());
        assertFalse(n.canConvertToExactIntegral());

        data[2] = (byte) 3;
        BinaryNode n2 = BinaryNode.valueOf(data, 2, 1);
        assertTrue(n.equals(n2));
        assertEquals("\"Aw==\"", n.toString());

        assertEquals("AAMD", new BinaryNode(data).asText());
        assertNodeNumbersForNonNumeric(n);

        assertNonContainerStreamMethods(n2);
    }

    @Test
    public void testPOJO()
    {
        POJONode n = new POJONode("x"); // not really a pojo but that's ok
        assertStandardEquals(n);
        assertEquals(n, new POJONode("x"));
        assertEquals("x", n.asText());
        // 10-Dec-2018, tatu: With 2.10, should serialize same as via ObjectMapper/ObjectWriter
        assertEquals("\"x\"", n.toString());

        assertEquals(new POJONode(null), new POJONode(null));

        // default; non-numeric
        assertNodeNumbersForNonNumeric(n);
        // but if wrapping actual number, use it
        assertNodeNumbers(new POJONode(Integer.valueOf(123)), 123, 123.0);

        assertNonContainerStreamMethods(n);
    }

    // [databind#743]
    @Test
    public void testRawValue() throws Exception
    {
        ObjectNode root = MAPPER.createObjectNode();
        root.putRawValue("a", new RawValue(new SerializedString("[1, 2, 3]")));

        assertEquals("{\"a\":[1, 2, 3]}", MAPPER.writeValueAsString(root));
    }

    // [databind#790]
    @Test
    public void testCustomComparators() throws Exception
    {
        ObjectNode nestedObject1 = MAPPER.createObjectNode();
        nestedObject1.put("value", 6);
        ArrayNode nestedArray1 = MAPPER.createArrayNode();
        nestedArray1.add(7);
        ObjectNode root1 = MAPPER.createObjectNode();
        root1.put("value", 5);
        root1.set("nested_object", nestedObject1);
        root1.set("nested_array", nestedArray1);

        ObjectNode nestedObject2 = MAPPER.createObjectNode();
        nestedObject2.put("value", 6.9);
        ArrayNode nestedArray2 = MAPPER.createArrayNode();
        nestedArray2.add(7.0);
        ObjectNode root2 = MAPPER.createObjectNode();
        root2.put("value", 5.0);
        root2.set("nested_object", nestedObject2);
        root2.set("nested_array", nestedArray2);

        // default equals(): not strictly equal
        assertFalse(root1.equals(root2));
        assertFalse(root2.equals(root1));
        assertTrue(root1.equals(root1));
        assertTrue(root2.equals(root2));

        assertTrue(nestedArray1.equals(nestedArray1));
        assertFalse(nestedArray1.equals(nestedArray2));
        assertFalse(nestedArray2.equals(nestedArray1));

        // but. Custom comparator can make all the difference
        Comparator<JsonNode> cmp = new Comparator<JsonNode>() {

            @Override
            public int compare(JsonNode o1, JsonNode o2) {
                if (o1 instanceof ContainerNode || o2 instanceof ContainerNode) {
                    fail("container nodes should be traversed, comparator should not be invoked");
                }
                if (o1.equals(o2)) {
                    return 0;
                }
                if ((o1 instanceof NumericNode) && (o2 instanceof NumericNode)) {
                    int d1 = ((NumericNode) o1).asInt();
                    int d2 = ((NumericNode) o2).asInt();
                    if (d1 == d2) { // strictly equals because it's integral value
                        return 0;
                    }
                    if (d1 < d2) {
                        return -1;
                    }
                    return 1;
                }
                return 0;
            }
        };
        assertTrue(root1.equals(cmp, root2));
        assertTrue(root2.equals(cmp, root1));
        assertTrue(root1.equals(cmp, root1));
        assertTrue(root2.equals(cmp, root2));

        ArrayNode array3 = MAPPER.createArrayNode();
        array3.add(123);

        assertFalse(root2.equals(cmp, nestedArray1));
        assertTrue(nestedArray1.equals(cmp, nestedArray1));
        assertFalse(nestedArray1.equals(cmp, root2));
        assertFalse(nestedArray1.equals(cmp, array3));
    }

    // [databind#793]
    @Test
    public void testArrayWithDefaultTyping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance);

        JsonNode array = mapper.readTree("[ 1, 2 ]");
        assertTrue(array.isArray());
        assertEquals(2, array.size());

        JsonNode obj = mapper.readTree("{ \"a\" : 2 }");
        assertTrue(obj.isObject());
        assertEquals(1, obj.size());
        assertEquals(2, obj.path("a").asInt());
    }

    // [databind#2145]
    @Test
    public void testOptionalAccessorOnArray() throws Exception {
        ArrayNode arrayNode = MAPPER.createArrayNode();
        arrayNode.add("firstElement");
        assertTrue(arrayNode.optional(0).isPresent());
        assertEquals("firstElement", arrayNode.optional(0).get().asText());
        assertFalse(arrayNode.optional(1).isPresent());
        assertFalse(arrayNode.optional(-1).isPresent());
        assertFalse(arrayNode.optional(999).isPresent());
        assertFalse(arrayNode.optional("anyField").isPresent());
    }

    @Test
    public void testOptionalAccessorOnObject() throws Exception {
        ObjectNode objectNode = MAPPER.createObjectNode();
        objectNode.put("existingField", "value");
        assertTrue(objectNode.optional("existingField").isPresent());
        assertEquals("value", objectNode.optional("existingField").get().asText());
        assertFalse(objectNode.optional("missingField").isPresent());
        assertFalse(objectNode.optional(0).isPresent());
        assertFalse(objectNode.optional(-1).isPresent());
    }

    @Test
    public void testOptionalAccessorOnNumbers() throws Exception
    {
        // Test IntNode
        IntNode intNode = IntNode.valueOf(42);
        assertFalse(intNode.optional("anyField").isPresent());
        assertFalse(intNode.optional(0).isPresent());

        // Test LongNode
        LongNode longNode = LongNode.valueOf(123456789L);
        assertFalse(longNode.optional("anyField").isPresent());
        assertFalse(longNode.optional(0).isPresent());

        // Test DoubleNode
        DoubleNode doubleNode = DoubleNode.valueOf(3.14);
        assertFalse(doubleNode.optional("anyField").isPresent());
        assertFalse(doubleNode.optional(0).isPresent());

        // Test DecimalNode
        DecimalNode decimalNode = DecimalNode.valueOf(new java.math.BigDecimal("12345.6789"));
        assertFalse(decimalNode.optional("anyField").isPresent());
        assertFalse(decimalNode.optional(0).isPresent());
    }

    @Test
    public void testOptionalAccessorOnOtherTypes() throws Exception
    {
        // Test TextNode
        TextNode textNode = TextNode.valueOf("sampleText");
        assertFalse(textNode.optional("anyField").isPresent());
        assertFalse(textNode.optional(0).isPresent());

        // Test NullNode
        NullNode nullNode = NullNode.getInstance();
        assertFalse(nullNode.optional("anyField").isPresent());
        assertFalse(nullNode.optional(0).isPresent());

        // Test BooleanNode
        BooleanNode booleanNode = BooleanNode.TRUE;
        assertFalse(booleanNode.optional("anyField").isPresent());
        assertFalse(booleanNode.optional(0).isPresent());
    }

    // [databind#4867]
    @Test
    public void testAsOptional() {
        // Test with MissingNode
        JsonNode missingNode = MissingNode.getInstance();
        Optional<JsonNode> missingOptional = missingNode.asOptional();
        assertFalse(missingOptional.isPresent());

        // Test with ObjectNode
        ObjectNode objectNode = MAPPER.createObjectNode();
        Optional<JsonNode> objectOptional = objectNode.asOptional();
        assertTrue(objectOptional.isPresent());
        assertEquals(objectNode, objectOptional.get());

        // Test with ArrayNode
        ArrayNode arrayNode = MAPPER.createArrayNode();
        Optional<JsonNode> arrayOptional = arrayNode.asOptional();
        assertTrue(arrayOptional.isPresent());
        assertEquals(arrayNode, arrayOptional.get());

        // Test with TextNode
        TextNode textNode = TextNode.valueOf("text");
        Optional<JsonNode> textOptional = textNode.asOptional();
        assertTrue(textOptional.isPresent());
        assertEquals(textNode, textOptional.get());

        // Test with NullNode
        NullNode nullNode = NullNode.getInstance();
        Optional<JsonNode> nullOptional = nullNode.asOptional();
        assertTrue(nullOptional.isPresent());
        assertEquals(nullNode, nullOptional.get());
    }
}

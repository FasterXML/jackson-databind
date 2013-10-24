package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.*;

/**
 * Basic tests for {@link JsonNode} implementations that
 * contain numeric values.
 */
public class TestNumberNodes extends NodeTestBase
{
    public void testShort()
    {
        ShortNode n = ShortNode.valueOf((short) 1);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.INT, n.numberType());	// should be SHORT
        assertEquals(1, n.intValue());
        assertEquals(1L, n.longValue());
        assertEquals(BigDecimal.ONE, n.decimalValue());
        assertEquals(BigInteger.ONE, n.bigIntegerValue());
        assertEquals("1", n.asText());

        assertNodeNumbers(n, 1, 1.0);

        assertTrue(ShortNode.valueOf((short) 0).canConvertToInt());
        assertTrue(ShortNode.valueOf(Short.MAX_VALUE).canConvertToInt());
        assertTrue(ShortNode.valueOf(Short.MIN_VALUE).canConvertToInt());

        assertTrue(ShortNode.valueOf((short) 0).canConvertToLong());
        assertTrue(ShortNode.valueOf(Short.MAX_VALUE).canConvertToLong());
        assertTrue(ShortNode.valueOf(Short.MIN_VALUE).canConvertToLong());
    }
    
	public void testInt()
    {
        IntNode n = IntNode.valueOf(1);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.INT, n.numberType());
        assertEquals(1, n.intValue());
        assertEquals(1L, n.longValue());
        assertEquals(BigDecimal.ONE, n.decimalValue());
        assertEquals(BigInteger.ONE, n.bigIntegerValue());
        assertEquals("1", n.asText());

        assertNodeNumbers(n, 1, 1.0);

        assertTrue(IntNode.valueOf(0).canConvertToInt());
        assertTrue(IntNode.valueOf(Integer.MAX_VALUE).canConvertToInt());
        assertTrue(IntNode.valueOf(Integer.MIN_VALUE).canConvertToInt());

        assertTrue(IntNode.valueOf(0).canConvertToLong());
        assertTrue(IntNode.valueOf(Integer.MAX_VALUE).canConvertToLong());
        assertTrue(IntNode.valueOf(Integer.MIN_VALUE).canConvertToLong());
    }

    public void testLong()
    {
        LongNode n = LongNode.valueOf(1L);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.LONG, n.numberType());
        assertEquals(1, n.intValue());
        assertEquals(1L, n.longValue());
        assertEquals(BigDecimal.ONE, n.decimalValue());
        assertEquals(BigInteger.ONE, n.bigIntegerValue());
        assertEquals("1", n.asText());

        assertNodeNumbers(n, 1, 1.0);

        // ok if contains small enough value
        assertTrue(LongNode.valueOf(0).canConvertToInt());
        assertTrue(LongNode.valueOf(Integer.MAX_VALUE).canConvertToInt());
        assertTrue(LongNode.valueOf(Integer.MIN_VALUE).canConvertToInt());
        // but not in other cases
        assertFalse(LongNode.valueOf(1L + Integer.MAX_VALUE).canConvertToInt());
        assertFalse(LongNode.valueOf(-1L + Integer.MIN_VALUE).canConvertToInt());

        assertTrue(LongNode.valueOf(0L).canConvertToLong());
        assertTrue(LongNode.valueOf(Long.MAX_VALUE).canConvertToLong());
        assertTrue(LongNode.valueOf(Long.MIN_VALUE).canConvertToLong());
    }

    public void testDouble() throws Exception
    {
        DoubleNode n = DoubleNode.valueOf(0.25);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, n.asToken());
        assertEquals(JsonParser.NumberType.DOUBLE, n.numberType());
        assertEquals(0, n.intValue());
        assertEquals(0.25, n.doubleValue());
        assertNotNull(n.decimalValue());
        assertEquals(BigInteger.ZERO, n.bigIntegerValue());
        assertEquals("0.25", n.asText());

        // 1.6:
        assertNodeNumbers(DoubleNode.valueOf(4.5), 4, 4.5);

        assertTrue(DoubleNode.valueOf(0).canConvertToInt());
        assertTrue(DoubleNode.valueOf(Integer.MAX_VALUE).canConvertToInt());
        assertTrue(DoubleNode.valueOf(Integer.MIN_VALUE).canConvertToInt());
        assertFalse(DoubleNode.valueOf(1L + Integer.MAX_VALUE).canConvertToInt());
        assertFalse(DoubleNode.valueOf(-1L + Integer.MIN_VALUE).canConvertToInt());

        assertTrue(DoubleNode.valueOf(0L).canConvertToLong());
        assertTrue(DoubleNode.valueOf(Long.MAX_VALUE).canConvertToLong());
        assertTrue(DoubleNode.valueOf(Long.MIN_VALUE).canConvertToLong());

        JsonNode num = objectMapper().readTree(" -0.0");
        assertTrue(num.isDouble());
        n = (DoubleNode) num;
        assertEquals(-0.0, n.doubleValue());
        assertEquals("-0.0", String.valueOf(n.doubleValue()));
    }

    // @since 2.2
    public void testFloat()
    {
        FloatNode n = FloatNode.valueOf(0.25f);
        assertStandardEquals(n);
        assertTrue(0 != n.hashCode());
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, n.asToken());
        assertEquals(JsonParser.NumberType.FLOAT, n.numberType());
        assertEquals(0, n.intValue());
        assertEquals(0.25, n.doubleValue());
        assertEquals(0.25f, n.floatValue());
        assertNotNull(n.decimalValue());
        assertEquals(BigInteger.ZERO, n.bigIntegerValue());
        assertEquals("0.25", n.asText());

        // 1.6:
        assertNodeNumbers(FloatNode.valueOf(4.5f), 4, 4.5f);

        assertTrue(FloatNode.valueOf(0).canConvertToInt());
        assertTrue(FloatNode.valueOf(Integer.MAX_VALUE).canConvertToInt());
        assertTrue(FloatNode.valueOf(Integer.MIN_VALUE).canConvertToInt());

        // rounding errors if we just add/sub 1... so:
        assertFalse(FloatNode.valueOf(1000L + Integer.MAX_VALUE).canConvertToInt());
        assertFalse(FloatNode.valueOf(-1000L + Integer.MIN_VALUE).canConvertToInt());

        assertTrue(FloatNode.valueOf(0L).canConvertToLong());
        assertTrue(FloatNode.valueOf(Integer.MAX_VALUE).canConvertToLong());
        assertTrue(FloatNode.valueOf(Integer.MIN_VALUE).canConvertToLong());
    }
    
    public void testDecimalNode() throws Exception
    {
        DecimalNode n = DecimalNode.valueOf(BigDecimal.ONE);
        assertStandardEquals(n);
        assertTrue(n.equals(new DecimalNode(BigDecimal.ONE)));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, n.asToken());
        assertEquals(JsonParser.NumberType.BIG_DECIMAL, n.numberType());
        assertTrue(n.isNumber());
        assertFalse(n.isIntegralNumber());
        assertTrue(n.isBigDecimal());
        assertEquals(BigDecimal.ONE, n.numberValue());
        assertEquals(1, n.intValue());
        assertEquals(1L, n.longValue());
        assertEquals(BigDecimal.ONE, n.decimalValue());
        assertEquals("1", n.asText());

        // 1.6:
        assertNodeNumbers(n, 1, 1.0);

        assertTrue(DecimalNode.valueOf(BigDecimal.ZERO).canConvertToInt());
        assertTrue(DecimalNode.valueOf(BigDecimal.valueOf(Integer.MAX_VALUE)).canConvertToInt());
        assertTrue(DecimalNode.valueOf(BigDecimal.valueOf(Integer.MIN_VALUE)).canConvertToInt());
        assertFalse(DecimalNode.valueOf(BigDecimal.valueOf(1L + Integer.MAX_VALUE)).canConvertToInt());
        assertFalse(DecimalNode.valueOf(BigDecimal.valueOf(-1L + Integer.MIN_VALUE)).canConvertToInt());

        assertTrue(DecimalNode.valueOf(BigDecimal.ZERO).canConvertToLong());
        assertTrue(DecimalNode.valueOf(BigDecimal.valueOf(Long.MAX_VALUE)).canConvertToLong());
        assertTrue(DecimalNode.valueOf(BigDecimal.valueOf(Long.MIN_VALUE)).canConvertToLong());
    }

    public void testBigIntegerNode() throws Exception
    {
        BigIntegerNode n = BigIntegerNode.valueOf(BigInteger.ONE);
        assertStandardEquals(n);
        assertTrue(n.equals(new BigIntegerNode(BigInteger.ONE)));
        assertEquals(JsonToken.VALUE_NUMBER_INT, n.asToken());
        assertEquals(JsonParser.NumberType.BIG_INTEGER, n.numberType());
        assertTrue(n.isNumber());
        assertTrue(n.isIntegralNumber());
        assertTrue(n.isBigInteger());
        assertEquals(BigInteger.ONE, n.numberValue());
        assertEquals(1, n.intValue());
        assertEquals(1L, n.longValue());
        assertEquals(BigInteger.ONE, n.bigIntegerValue());
        assertEquals("1", n.asText());
        
        // 1.6:
        assertNodeNumbers(n, 1, 1.0);

        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        
        n = BigIntegerNode.valueOf(maxLong);
        assertEquals(Long.MAX_VALUE, n.longValue());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode n2 = mapper.readTree(maxLong.toString());
        assertEquals(Long.MAX_VALUE, n2.longValue());

        // then over long limit:
        BigInteger beyondLong = maxLong.shiftLeft(2); // 4x max long
        n2 = mapper.readTree(beyondLong.toString());
        assertEquals(beyondLong, n2.bigIntegerValue());

        assertTrue(BigIntegerNode.valueOf(BigInteger.ZERO).canConvertToInt());
        assertTrue(BigIntegerNode.valueOf(BigInteger.valueOf(Integer.MAX_VALUE)).canConvertToInt());
        assertTrue(BigIntegerNode.valueOf(BigInteger.valueOf(Integer.MIN_VALUE)).canConvertToInt());
        assertFalse(BigIntegerNode.valueOf(BigInteger.valueOf(1L + Integer.MAX_VALUE)).canConvertToInt());
        assertFalse(BigIntegerNode.valueOf(BigInteger.valueOf(-1L + Integer.MIN_VALUE)).canConvertToInt());

        assertTrue(BigIntegerNode.valueOf(BigInteger.ZERO).canConvertToLong());
        assertTrue(BigIntegerNode.valueOf(BigInteger.valueOf(Long.MAX_VALUE)).canConvertToLong());
        assertTrue(BigIntegerNode.valueOf(BigInteger.valueOf(Long.MIN_VALUE)).canConvertToLong());
    }

    public void testBigDecimalAsPlain() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        final String INPUT = "{\"x\":1e2}";
        final JsonNode node = mapper.readTree(INPUT);
        String result = mapper.writeValueAsString(node);
        assertEquals("{\"x\":100}", result);

        // also via ObjectWriter:
        assertEquals("{\"x\":100}", mapper.writer().writeValueAsString(node));
    }

    // Related to [Issue#333]
    public void testCanonicalNumbers() throws Exception
    {
        JsonNodeFactory f = new JsonNodeFactory();
        NumericNode n = f.numberNode(123);
        assertTrue(n.isInt());
        n = f.numberNode(1L + Integer.MAX_VALUE);
        assertFalse(n.isInt());
        assertTrue(n.isLong());
        // but "too small" number will be 'int'...
        n = f.numberNode(123L);
        assertTrue(n.isInt());
        assertFalse(n.isLong());
    }
}

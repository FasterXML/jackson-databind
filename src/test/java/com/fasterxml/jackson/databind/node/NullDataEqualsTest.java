package com.fasterxml.jackson.databind.node;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class NullDataEqualsTest {
    @Test
    void testNullBinaryNode() {
        assertEquals(new BinaryNode(null), new BinaryNode(null));
        assertNotEquals(new BinaryNode(new byte[8]), new BinaryNode(null));
        assertNotEquals(new BinaryNode(null), new BinaryNode(new byte[8]));
    }

    @Test
    void testNullBigIntegerNode() {
        assertEquals(new BigIntegerNode(null), new BigIntegerNode(null));
        assertNotEquals(new BigIntegerNode(BigInteger.ZERO), new BigIntegerNode(null));
        assertNotEquals(new BigIntegerNode(null), new BigIntegerNode(BigInteger.ZERO));
    }

    @Test
    void testNullDecimalNode() {
        assertEquals(new DecimalNode(null), new DecimalNode(null));
        assertNotEquals(new DecimalNode(BigDecimal.ZERO), new DecimalNode(null));
        assertNotEquals( new DecimalNode(null), new DecimalNode(BigDecimal.ZERO));
    }
}

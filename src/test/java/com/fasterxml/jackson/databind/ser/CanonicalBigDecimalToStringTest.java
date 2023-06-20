package com.fasterxml.jackson.databind.ser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class CanonicalBigDecimalToStringTest {

    @Test
    void testCanonicalDecimalHandling_1() throws Exception {
        assertSerialized("1", new BigDecimal("1"));
    }

    @Test
    void testCanonicalDecimalHandling_1_000() throws Exception {
        assertSerialized("1", new BigDecimal("1.000"));
    }

    @Test
    void testCanonicalDecimalHandling_10_1000() throws Exception {
        assertSerialized("1.01E1", new BigDecimal("10.1000"));
    }

    @Test
    void testCanonicalDecimalHandling_1000() throws Exception {
        assertSerialized("1E3", new BigDecimal("1000"));
    }

    @Test
    void testCanonicalDecimalHandling_0_00000000010() throws Exception {
        assertSerialized("0.0000000001", new BigDecimal("0.00000000010"));
    }

    @Test
    void testCanonicalDecimalHandling_1000_00010() throws Exception {
        assertSerialized("1.0000001E3", new BigDecimal("1000.00010"));
    }

    @Test
    void testCanonicalHugeDecimalHandling() throws Exception {
        BigDecimal actual = new BigDecimal("123456789123456789123456789123456789.123456789123456789123456789123456789123456789000");
        assertSerialized("1.23456789123456789123456789123456789123456789123456789123456789123456789123456789E35", actual);
    }

    private void assertSerialized(String expected, BigDecimal actual) {
        CanonicalBigDecimalToString serializer = new CanonicalBigDecimalToString();
        assertEquals(expected, serializer.convert(actual));
    }

}

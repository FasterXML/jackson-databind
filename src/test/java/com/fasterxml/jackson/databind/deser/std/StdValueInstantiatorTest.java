package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.databind.BaseMapTest;

import java.math.BigDecimal;

public class StdValueInstantiatorTest extends BaseMapTest {

    public void testDoubleValidation_valid() {
        assertTrue(StdValueInstantiator.canConvertToDouble(BigDecimal.ZERO));
        assertTrue(StdValueInstantiator.canConvertToDouble(BigDecimal.ONE));
        assertTrue(StdValueInstantiator.canConvertToDouble(BigDecimal.TEN));
        assertTrue(StdValueInstantiator.canConvertToDouble(BigDecimal.valueOf(-1.5D)));
    }

    public void testDoubleValidation_invalid() {
        BigDecimal value = BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE));
        assertFalse(StdValueInstantiator.canConvertToDouble(value));
    }
}
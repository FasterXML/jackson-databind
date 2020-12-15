package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.databind.BaseMapTest;

import java.math.BigDecimal;

// [databind#2978]
public class StdValueInstantiatorTest extends BaseMapTest
{
    public void testDoubleValidation_valid() {
        assertEquals(0d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ZERO));
        assertEquals(1d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ONE));
        assertEquals(10d, StdValueInstantiator.tryConvertToDouble(BigDecimal.TEN));
        assertEquals(-1.5d, StdValueInstantiator.tryConvertToDouble(BigDecimal.valueOf(-1.5d)));
    }

    public void testDoubleValidation_invalid() {
        BigDecimal value = BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE));
        assertNull(StdValueInstantiator.tryConvertToDouble(value));
    }
}

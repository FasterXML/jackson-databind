package tools.jackson.databind.deser.std;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#2978]
public class StdValueInstantiatorTest
{
    @Test
    public void testDoubleValidation_valid() {
        assertEquals(0d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ZERO));
        assertEquals(1d, StdValueInstantiator.tryConvertToDouble(BigDecimal.ONE));
        assertEquals(10d, StdValueInstantiator.tryConvertToDouble(BigDecimal.TEN));
        assertEquals(-1.5d, StdValueInstantiator.tryConvertToDouble(BigDecimal.valueOf(-1.5d)));
    }

    @Test
    public void testDoubleValidation_invalid() {
        BigDecimal value = BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE));
        assertNull(StdValueInstantiator.tryConvertToDouble(value));
    }
}

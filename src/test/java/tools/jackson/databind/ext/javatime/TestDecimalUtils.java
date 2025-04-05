package tools.jackson.databind.ext.javatime;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import tools.jackson.databind.ext.javatime.util.DecimalUtils;

import static org.junit.jupiter.api.Assertions.*;

public class TestDecimalUtils extends DateTimeTestBase
{
    @Test
    public void testToDecimal01()
    {
        String decimal = DecimalUtils.toDecimal(0, 0);
        assertEquals(NO_NANOSECS_SER, decimal, "The returned decimal is not correct.");

        decimal = DecimalUtils.toDecimal(15, 72);
        assertEquals("15.000000072", decimal, "The returned decimal is not correct.");

        decimal = DecimalUtils.toDecimal(19827342231L, 192837465);
        assertEquals("19827342231.192837465", decimal, "The returned decimal is not correct.");

        decimal = DecimalUtils.toDecimal(19827342231L, 0);
        assertEquals("19827342231"+NO_NANOSECS_SUFFIX, decimal,
                "The returned decimal is not correct.");

        decimal = DecimalUtils.toDecimal(19827342231L, 999888000);
        assertEquals("19827342231.999888000", decimal,
                "The returned decimal is not correct.");

        decimal = DecimalUtils.toDecimal(-22704862, 599000000);
        assertEquals("-22704862.599000000", decimal,
                "The returned decimal is not correct.");
    }

    private void checkExtractNanos(long expectedSeconds, int expectedNanos, BigDecimal decimal)
    {
        long seconds = decimal.longValue();
        assertEquals(expectedSeconds, seconds, "The second part is not correct.");
    }

    @Test
    public void testExtractNanosecondDecimal01()
    {
        BigDecimal value = new BigDecimal("0");
        checkExtractNanos(0L, 0, value);
    }

    @Test
    public void testExtractNanosecondDecimal02()
    {
        BigDecimal value = new BigDecimal("15.000000072");
        checkExtractNanos(15L, 72, value);
    }

    @Test
    public void testExtractNanosecondDecimal03()
    {
        BigDecimal value = new BigDecimal("15.72");
        checkExtractNanos(15L, 720000000, value);
    }

    @Test
    public void testExtractNanosecondDecimal04()
    {
        BigDecimal value = new BigDecimal("19827342231.192837465");
        checkExtractNanos(19827342231L, 192837465, value);
    }

    @Test
    public void testExtractNanosecondDecimal05()
    {
        BigDecimal value = new BigDecimal("19827342231");
        checkExtractNanos(19827342231L, 0, value);
    }

    @Test
    public void testExtractNanosecondDecimal06()
    {
        BigDecimal value = new BigDecimal("19827342231.999999999");
        checkExtractNanos(19827342231L, 999999999, value);
    }

    private void checkExtractSecondsAndNanos(long expectedSeconds, int expectedNanos, BigDecimal decimal)
    {
        DecimalUtils.extractSecondsAndNanos(decimal, (Long s, Integer ns) -> {
            assertEquals(expectedSeconds, s.longValue(), "The second part is not correct.");
            assertEquals(expectedNanos, ns.intValue(), "The nanosecond part is not correct.");
            return null;
        }, true);
    }

    @Test
    public void testExtractSecondsAndNanos01()
    {
        BigDecimal value = new BigDecimal("0");
        checkExtractSecondsAndNanos(0L, 0, value);
    }

    @Test
    public void testExtractSecondsAndNanos02()
    {
        BigDecimal value = new BigDecimal("15.000000072");
        checkExtractSecondsAndNanos(15L, 72, value);
    }

    @Test
    public void testExtractSecondsAndNanos03()
    {
        BigDecimal value = new BigDecimal("15.72");
        checkExtractSecondsAndNanos(15L, 720000000, value);
    }

    @Test
    public void testExtractSecondsAndNanos04()
    {
        BigDecimal value = new BigDecimal("19827342231.192837465");
        checkExtractSecondsAndNanos(19827342231L, 192837465, value);
    }

    @Test
    public void testExtractSecondsAndNanos05()
    {
        BigDecimal value = new BigDecimal("19827342231");
        checkExtractSecondsAndNanos(19827342231L, 0, value);
    }

    @Test
    public void testExtractSecondsAndNanos06()
    {
        BigDecimal value = new BigDecimal("19827342231.999999999");
        checkExtractSecondsAndNanos(19827342231L, 999999999, value);
    }

    @Test
    public void testExtractSecondsAndNanosFromNegativeBigDecimal()
    {
        BigDecimal value = new BigDecimal("-22704862.599000000");
        checkExtractSecondsAndNanos(-22704862L, 599000000, value);
    }

    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testExtractSecondsAndNanos07()
    {
        BigDecimal value = new BigDecimal("1e10000000");
        checkExtractSecondsAndNanos(0L, 0, value);
    }
}

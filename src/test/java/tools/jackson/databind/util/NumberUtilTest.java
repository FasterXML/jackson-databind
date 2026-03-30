package tools.jackson.databind.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @since 3.2
 */
public class NumberUtilTest
{
    @Test
    public void testSimpleDigits() {
        assertTrue(NumberUtil.isValidJDKIntNumber("0"));
        assertTrue(NumberUtil.isValidJDKIntNumber("1"));
        assertTrue(NumberUtil.isValidJDKIntNumber("42"));
        assertTrue(NumberUtil.isValidJDKIntNumber("1234567890"));
    }

    @Test
    public void testLeadingZeroes() {
        assertTrue(NumberUtil.isValidJDKIntNumber("007"));
        assertTrue(NumberUtil.isValidJDKIntNumber("00"));
    }

    @Test
    public void testSignedNumbers() {
        assertTrue(NumberUtil.isValidJDKIntNumber("-1"));
        assertTrue(NumberUtil.isValidJDKIntNumber("+1"));
        assertTrue(NumberUtil.isValidJDKIntNumber("-0"));
        assertTrue(NumberUtil.isValidJDKIntNumber("+42"));
        assertTrue(NumberUtil.isValidJDKIntNumber("-1234567890"));
    }

    @Test
    public void testEmptyAndSignOnly() {
        assertFalse(NumberUtil.isValidJDKIntNumber(""));
        assertFalse(NumberUtil.isValidJDKIntNumber("-"));
        assertFalse(NumberUtil.isValidJDKIntNumber("+"));
    }

    @Test
    public void testNonNumeric() {
        assertFalse(NumberUtil.isValidJDKIntNumber("abc"));
        assertFalse(NumberUtil.isValidJDKIntNumber("12a"));
        assertFalse(NumberUtil.isValidJDKIntNumber("a12"));
        assertFalse(NumberUtil.isValidJDKIntNumber("NOT_A_NUMBER"));
    }

    @Test
    public void testDecimalAndFloat() {
        assertFalse(NumberUtil.isValidJDKIntNumber("1.0"));
        assertFalse(NumberUtil.isValidJDKIntNumber("3.14"));
        assertFalse(NumberUtil.isValidJDKIntNumber("1e10"));
        assertFalse(NumberUtil.isValidJDKIntNumber("1E10"));
    }

    @Test
    public void testWhitespaceAndSpecialChars() {
        assertFalse(NumberUtil.isValidJDKIntNumber(" 1"));
        assertFalse(NumberUtil.isValidJDKIntNumber("1 "));
        assertFalse(NumberUtil.isValidJDKIntNumber(" "));
        assertFalse(NumberUtil.isValidJDKIntNumber("1,000"));
    }
}

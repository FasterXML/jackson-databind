package tools.jackson.databind.util;

import java.math.BigDecimal;

/**
 * Shared utility methods for number validation.
 *
 * @since 3.2
 */
public abstract class NumberUtil
{
    /**
     * Helper method to check whether given text refers to what looks like a clean simple
     * integer number, consisting of optional sign followed by a sequence of digits.
     *<p>
     * Note that definition is quite loose as leading zeroes are allowed, in addition
     * to plus sign (not just minus).
     *
     * @since 3.2 as {@code public static} (formerly {@code protected})
     */
    public static boolean isValidJDKIntNumber(String text)
    {
        final int len = text.length();
        if (len > 0) {
            char c = text.charAt(0);
            // skip leading sign (plus not allowed for strict JSON numbers but...)
            int i;

            if (c == '-' || c == '+') {
                if (len == 1) {
                    return false;
                }
                i = 1;
            } else {
                i = 0;
            }
            // We will allow leading 0, too
            for (; i < len; ++i) {
                int ch = text.charAt(i);
                if (ch > '9' || ch < '0') {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static BigDecimal stripTrailingZeros(BigDecimal nr) {
        // 24-Mar-2021, tatu: [dataformats-binary#264] barfs on a specific value...
        //   Must skip normalization in that particular case. Alas, haven't found
        //   another way to check it instead of getting "Overflow", catching
        try {
            return nr.stripTrailingZeros();
        } catch (ArithmeticException e) {
            // If we can't, we can't...
            return nr;
        }
    }
}

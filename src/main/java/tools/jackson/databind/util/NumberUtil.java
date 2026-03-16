package tools.jackson.databind.util;

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

}

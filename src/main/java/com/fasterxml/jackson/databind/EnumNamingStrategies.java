package com.fasterxml.jackson.databind;

/**
 * A container class for implementations of the {@link EnumNamingStrategy} interface.
 *
 * @since 2.15
 */
public class EnumNamingStrategies {

    private EnumNamingStrategies() {}

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to camel case format. This implementation follows three rules
     * described below.
     *
     * <ol>
     * <li>converts any character preceded by an underscore into upper case character,
     * regardless of its original case (upper or lower).</li>
     * <li>converts any character NOT preceded by an underscore into a lower case character,
     * regardless of its original case (upper or lower).</li>
     * <li>converts contiguous sequence of underscores into a single underscore.</li>
     * </ol>
     *
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanutButter".
     * And "peanutButter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * These rules result in the following example conversions from upper snakecase names
     * to camelcase names.
     * <ul>
     * <li>"USER_NAME" is converted into "userName"</li>
     * <li>"USER______NAME" is converted into "userName"</li>
     * <li>"USERNAME" is converted into "username"</li>
     * <li>"User__Name" is converted into "userName"</li>
     * <li>"_user_name" is converted into "UserName"</li>
     * <li>"_user_name_s" is converted into "UserNameS"</li>
     * <li>"__Username" is converted into "Username"</li>
     * <li>"__username" is converted into "Username"</li>
     * <li>"username" is converted into "username"</li>
     * <li>"Username" is converted into "username"</li>
     * </ul>
     *
     * @since 2.15
     */
    public static class CamelCaseStrategy implements EnumNamingStrategy {

        /**
         * An intance of {@link CamelCaseStrategy} for reuse.
         *
         * @since 2.15
         */
        public static final CamelCaseStrategy INSTANCE = new CamelCaseStrategy();

        /**
         * @since 2.15
         */
        @Override
        public String convertEnumToExternalName(String enumName) {
            if (enumName == null) {
                return null;
            }

            final String UNDERSCORE = "_";
            StringBuilder out = null;
            int iterationCnt = 0;
            int lastSeparatorIdx = -1;

            do {
                lastSeparatorIdx = indexIn(enumName, lastSeparatorIdx + 1);
                if (lastSeparatorIdx != -1) {
                    if (iterationCnt == 0) {
                        out = new StringBuilder(enumName.length() + 4 * UNDERSCORE.length());
                        out.append(toLowerCase(enumName.substring(iterationCnt, lastSeparatorIdx)));
                    } else {
                        out.append(normalizeWord(enumName.substring(iterationCnt, lastSeparatorIdx)));
                    }
                    iterationCnt = lastSeparatorIdx + UNDERSCORE.length();
                }
            } while (lastSeparatorIdx != -1);

            if (iterationCnt == 0) {
                return toLowerCase(enumName);
            }
            out.append(normalizeWord(enumName.substring(iterationCnt)));
            return out.toString();
        }

        private static int indexIn(CharSequence sequence, int start) {
            int length = sequence.length();
            for (int i = start; i < length; i++) {
                if ('_' == sequence.charAt(i)) {
                    return i;
                }
            }
            return -1;
        }

        private static String normalizeWord(String word) {
            int length = word.length();
            if (length == 0) {
                return word;
            }
            return new StringBuilder(length)
                    .append(charToUpperCaseIfLower(word.charAt(0)))
                    .append(toLowerCase(word.substring(1)))
                    .toString();
        }

        private static String toLowerCase(String string) {
            int length = string.length();
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append(charToLowerCaseIfUpper(string.charAt(i)));
            }
            return builder.toString();
        }

        private static char charToUpperCaseIfLower(char c) {
            return Character.isLowerCase(c) ? Character.toUpperCase(c) : c;
        }

        private static char charToLowerCaseIfUpper(char c) {
            return Character.isUpperCase(c) ? Character.toLowerCase(c) : c;
        }
    }
}

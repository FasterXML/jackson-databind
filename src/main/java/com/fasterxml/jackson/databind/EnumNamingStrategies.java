package com.fasterxml.jackson.databind;

/**
 * A container class for implementations of the {@link EnumNamingStrategy} interface.
 *
 * @since 2.15
 */
public class EnumNamingStrategies {

    private EnumNamingStrategies() {}

    /**
     * Words other than first are capitalized and no separator is used between words.
     * See {@link EnumNamingStrategies.LowerCamelCaseStrategy} for details.
     *<p>
     * Example external property names would be "numberValue", "namingStrategy", "theDefiniteProof".
     *
     * @since 2.19
     */
    public static final EnumNamingStrategy LOWER_CAMEL_CASE = LowerCamelCaseStrategy.INSTANCE;

    /**
     * Words are capitalized and no separator is used between words.
     * See {@link EnumNamingStrategies.UpperCamelCaseStrategy} for details.
     *<p>
     * Example external property names would be "NumberValue", "NamingStrategy", "TheDefiniteProof".
     *
     * @since 2.19
     */
    public static final EnumNamingStrategy UPPER_CAMEL_CASE = UpperCamelCaseStrategy.INSTANCE;

    /**
     * @since 2.15
     * @deprecated Since 2.19 use {@link LowerCamelCaseStrategy} instead.
     */
    @Deprecated
    public static class CamelCaseStrategy implements EnumNamingStrategy {
        /**
         * An instance of {@link LowerCamelCaseStrategy} for reuse.
         *
         * @since 2.15
         */
        public static final LowerCamelCaseStrategy INSTANCE = new LowerCamelCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return LOWER_CAMEL_CASE.convertEnumToExternalName(enumName);
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to lower camel case format. This implementation follows three rules
     * described below.
     *
     * <ol>
     * <li>converts any character preceded by an underscore into upper case character,
     * regardless of its original case (upper or lower).</li>
     * <li>converts any character NOT preceded by an underscore into a lower case character,
     * regardless of its original case (upper or lower).</li>
     * <li>removes all underscores.</li>
     * </ol>
     *
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanutButter".
     * And "peanutButter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * These rules result in the following example conversions from upper snakecase names
     * to lower camelcase names.
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
     * @since 2.19
     */
    public static class LowerCamelCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerCamelCaseStrategy} for reuse.
         *
         * @since 2.19
         */
        public static final LowerCamelCaseStrategy INSTANCE = new LowerCamelCaseStrategy();

        /**
         * @since 2.19
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
                    .append(Character.toUpperCase(word.charAt(0)))
                    .append(toLowerCase(word.substring(1)))
                    .toString();
        }

        private static String toLowerCase(String string) {
            int length = string.length();
            StringBuilder builder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                builder.append(Character.toLowerCase(string.charAt(i)));
            }
            return builder.toString();
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format. This implementation follows three rules
     * described below.
     *
     * <ol>
     * <li>converts any character preceded by an underscore into upper case character,
     * regardless of its original case (upper or lower).</li>
     * <li>converts any character NOT preceded by an underscore into a lower case character,
     * regardless of its original case (upper or lower).</li>
     * <li>converts the first char in the string, regardless of any underscores, to uppercase</li>
     * <li>removes all underscores.</li>
     * </ol>
     *
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "PeanutButter".
     * And "PeanutButter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * These rules result in the following example conversions from upper snakecase names
     * to upper camelcase names.
     * <ul>
     * <li>"USER_NAME" is converted into "UserName"</li>
     * <li>"USER______NAME" is converted into "UserName"</li>
     * <li>"USERNAME" is converted into "Username"</li>
     * <li>"User__Name" is converted into "UserName"</li>
     * <li>"_user_name" is converted into "UserName"</li>
     * <li>"_user_name_s" is converted into "UserNameS"</li>
     * <li>"__Username" is converted into "Username"</li>
     * <li>"__username" is converted into "Username"</li>
     * <li>"username" is converted into "Username"</li>
     * <li>"Username" is converted into "Username"</li>
     * </ul>
     *
     * @since 2.19
     */
    public static class UpperCamelCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerCamelCaseStrategy} for reuse.
         *
         * @since 2.19
         */
        public static final UpperCamelCaseStrategy INSTANCE = new UpperCamelCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            String lowerCamelCase = LOWER_CAMEL_CASE.convertEnumToExternalName(enumName);
            return PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE.translate(lowerCamelCase);
        }
    }
}

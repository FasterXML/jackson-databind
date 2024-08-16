package com.fasterxml.jackson.databind;

import java.util.Locale;

/**
 * A container class for implementations of the {@link EnumNamingStrategy} interface.
 *
 * @since 2.15
 */
public class EnumNamingStrategies
{
    private EnumNamingStrategies() { }

    /**
     * Words other than first are capitalized and no separator is used between words.
     * See {@link EnumNamingStrategies.LowerCamelCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "enumName".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy LOWER_CAMEL_CASE = LowerCamelCaseStrategy.INSTANCE;

    /**
     * Naming convention used in languages like Pascal, where all words are capitalized and no separator is used between
     * words.
     * See {@link EnumNamingStrategies.UpperCamelCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "EnumName".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy UPPER_CAMEL_CASE = UpperCamelCaseStrategy.INSTANCE;

    /**
     * Naming convention used in languages like C, where words are in lower-case letters, separated by underscores.
     * See {@link EnumNamingStrategies.SnakeCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "enum_name".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy SNAKE_CASE = SnakeCaseStrategy.INSTANCE;

    /**
     * Naming convention in which the words are in upper-case letters, separated by underscores.
     * See {@link EnumNamingStrategies.UpperSnakeCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "ENUM_NAME", but "__ENUM_NAME_" would also be converted to "ENUM_NAME".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy UPPER_SNAKE_CASE = UpperSnakeCaseStrategy.INSTANCE;

    /**
     * Naming convention in which all words of the logical name are in lower case, and no separator is used between words.
     * See {@link EnumNamingStrategies.LowerCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "enumname".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy LOWER_CASE = LowerCaseStrategy.INSTANCE;

    /**
     * Naming convention used in languages like Lisp, where words are in lower-case letters, separated by hyphens.
     * See {@link EnumNamingStrategies.KebabCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "enum-name".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy KEBAB_CASE = KebabCaseStrategy.INSTANCE;

    /**
     * Naming convention widely used as configuration properties name, where words are in lower-case letters,
     * separated by dots.
     * See {@link EnumNamingStrategies.LowerDotCaseStrategy} for details.
     * <p>
     * Example "ENUM_NAME" would be converted to "enum.name".
     *
     * @since 2.18
     */
    public static final EnumNamingStrategy LOWER_DOT_CASE = LowerDotCaseStrategy.INSTANCE;

    /**
     * @since 2.15
     * @deprecated Since 2.18 use {@link LowerCamelCaseStrategy} instead.
     */
    @Deprecated
    public static class CamelCaseStrategy implements EnumNamingStrategy {
        /**
         * An instance of {@link CamelCaseStrategy} for reuse.
         *
         * @since 2.15
         */
        public static final CamelCaseStrategy INSTANCE = new CamelCaseStrategy();

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
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanutButter".
     * And "peanutButter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
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
     * @since 2.18
     */
    public static class LowerCamelCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerCamelCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final LowerCamelCaseStrategy INSTANCE = new LowerCamelCaseStrategy();

        /**
         * @since 2.18
         */
        @Override
        public String convertEnumToExternalName(String enumName) {
            return toBeanName(enumName);
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.UpperCamelCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "PeanutButter".
     * And "PeanutButter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
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
     * @since 2.18
     */
    public static class UpperCamelCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerCamelCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final UpperCamelCaseStrategy INSTANCE = new UpperCamelCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.SnakeCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanut_butter".
     * And "peanut_butter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
     * <ul>
     * <li>"USER_NAME" is converted into "user_name"</li>
     * <li>"USER______NAME" is converted into "user_name"</li>
     * <li>"USERNAME" is converted into "username"</li>
     * <li>"User__Name" is converted into "user_name"</li>
     * <li>"_user_name" is converted into "user_name"</li>
     * <li>"_user_name_s" is converted into "user_name_s"</li>
     * <li>"__Username" is converted into "username"</li>
     * <li>"__username" is converted into "username"</li>
     * <li>"username" is converted into "username"</li>
     * <li>"Username" is converted into "username"</li>
     * </ul>
     *
     * @since 2.18
     */
    public static class SnakeCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link SnakeCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final SnakeCaseStrategy INSTANCE = new SnakeCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.UpperSnakeCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "PEANUT_BUTTER".
     * And "PEANUT_BUTTER" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
     * <ul>
     * <li>"USER_NAME" is converted into "USER_NAME"</li>
     * <li>"USER______NAME" is converted into "USER_NAME"</li>
     * <li>"USERNAME" is converted into "USERNAME"</li>
     * <li>"User__Name" is converted into "USER_NAME"</li>
     * <li>"_user_name" is converted into "USER_NAME"</li>
     * <li>"_user_name_s" is converted into "USER_NAME_S"</li>
     * <li>"__Username" is converted into "USERNAME"</li>
     * <li>"__username" is converted into "USERNAME"</li>
     * <li>"username" is converted into "USERNAME"</li>
     * <li>"Username" is converted into "USERNAME"</li>
     * </ul>
     *
     * @since 2.18
     */
    public static class UpperSnakeCaseStrategy extends SnakeCaseStrategy {

        /**
         * An instance of {@link UpperSnakeCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final UpperSnakeCaseStrategy INSTANCE = new UpperSnakeCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.LowerCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanutbutter".
     * And "peanutbutter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
     * <ul>
     * <li>"USER_NAME" is converted into "username"</li>
     * <li>"USER______NAME" is converted into "username"</li>
     * <li>"USERNAME" is converted into "username"</li>
     * <li>"User__Name" is converted into "username"</li>
     * <li>"_user_name" is converted into "username"</li>
     * <li>"_user_name_s" is converted into "usernames"</li>
     * <li>"__Username" is converted into "username"</li>
     * <li>"__username" is converted into "username"</li>
     * <li>"username" is converted into "username"</li>
     * <li>"Username" is converted into "username"</li>
     * </ul>
     *
     * @since 2.18
     */
    public static class LowerCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final LowerCaseStrategy INSTANCE = new LowerCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.LowerCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to upper camel case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.KebabCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanut-butter".
     * And "peanut-butter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
     * <ul>
     * <li>"USER_NAME" is converted into "user-name"</li>
     * <li>"USER______NAME" is converted into "user-name"</li>
     * <li>"USERNAME" is converted into "username"</li>
     * <li>"User__Name" is converted into "user-name"</li>
     * <li>"_user_name" is converted into "user-name"</li>
     * <li>"_user_name_s" is converted into "user-name-s"</li>
     * <li>"__Username" is converted into "username"</li>
     * <li>"__username" is converted into "username"</li>
     * <li>"username" is converted into "username"</li>
     * <li>"Username" is converted into "username"</li>
     * </ul>
     *
     * @since 2.18
     */
    public static class KebabCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link KebabCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final KebabCaseStrategy INSTANCE = new KebabCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.KebabCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * <p>
     * An implementation of {@link EnumNamingStrategy} that converts enum names in the typical upper
     * snake case format to lower dot case format.
     * This implementation first normalizes to lower camel case using (see {@link LowerCamelCaseStrategy} for details)
     * and then uses {@link PropertyNamingStrategies.LowerDotCaseStrategy} to finish converting the name.
     * <p>
     * WARNING: Naming conversion conflicts caused by underscore usage should be handled by client.
     * e.g. Both <code>PEANUT_BUTTER</code>, <code>PEANUT__BUTTER</code> are converted into "peanut.butter".
     * And "peanut.butter" will be deserialized into enum with smaller <code>Enum.ordinal()</code> value.
     *
     * <p>
     * This results in the following example conversions:
     * <ul>
     * <li>"USER_NAME" is converted into "user.name"</li>
     * <li>"USER______NAME" is converted into "user.name"</li>
     * <li>"USERNAME" is converted into "username"</li>
     * <li>"User__Name" is converted into "user.name"</li>
     * <li>"_user_name" is converted into "user.name"</li>
     * <li>"_user_name_s" is converted into "user.name.s"</li>
     * <li>"__Username" is converted into "username"</li>
     * <li>"__username" is converted into "username"</li>
     * <li>"username" is converted into "username"</li>
     * <li>"Username" is converted into "username"</li>
     * </ul>
     *
     * @since 2.18
     */
    public static class LowerDotCaseStrategy implements EnumNamingStrategy {

        /**
         * An instance of {@link LowerDotCaseStrategy} for reuse.
         *
         * @since 2.18
         */
        public static final LowerDotCaseStrategy INSTANCE = new LowerDotCaseStrategy();

        @Override
        public String convertEnumToExternalName(String enumName) {
            return PropertyNamingStrategies.LowerDotCaseStrategy.INSTANCE.translate(toBeanName(enumName));
        }
    }

    /**
     * Normalizes the enum name to lower camel case in order to be further processed by a PropertyNamingStrategy.
     *
     * @param enumName the enum name to be normalized
     * @return the normalized enum name
     */
    static String toBeanName(String enumName) {
        if (enumName == null) {
            return null;
        }

        final String UNDERSCORE = "_";
        StringBuilder out = null;
        int iterationCnt = 0;
        int lastSeparatorIdx = -1;

        do {
            lastSeparatorIdx = nextIndexOfUnderscore(enumName, lastSeparatorIdx + 1);
            if (lastSeparatorIdx != -1) {
                if (iterationCnt == 0) {
                    out = new StringBuilder(enumName.length() + 4 * UNDERSCORE.length());
                    out.append(enumName.substring(iterationCnt, lastSeparatorIdx).toLowerCase(Locale.ROOT));
                } else {
                    out.append(normalizeWord(enumName.substring(iterationCnt, lastSeparatorIdx)));
                }
                iterationCnt = lastSeparatorIdx + UNDERSCORE.length();
            }
        } while (lastSeparatorIdx != -1);

        if (iterationCnt == 0) {
            return enumName.toLowerCase(Locale.ROOT);
        }
        out.append(normalizeWord(enumName.substring(iterationCnt)));
        return out.toString();
    }

    private static int nextIndexOfUnderscore(CharSequence sequence, int start) {
        int length = sequence.length();
        for (int i = start; i < length; i++) {
            if ('_' == sequence.charAt(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts the first letter of the word to uppercase and the rest of the word to lowercase.
     */
    private static String normalizeWord(String word) {
        int length = word.length();
        if (length == 0) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}

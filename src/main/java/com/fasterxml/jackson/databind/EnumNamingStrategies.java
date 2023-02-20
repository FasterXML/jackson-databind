package com.fasterxml.jackson.databind;

public class EnumNamingStrategies {

    public static final EnumNamingStrategy CAMEL_CASE = CamelCaseStrategy.INSTANCE;

    /**
     * no-op naming. Does nothing
     */
    public static class NoOpEnumNamingStrategy implements EnumNamingStrategy {

        @Override
        public String translate(String value) {
            return value;
        }

    }

    /**
     * <p>
     * Used when external value is in conventional CamelCase. Examples are "numberValue", "namingStrategy", "theDefiniteProof".
     * First underscore prefix will always be removed.
     */
    public static class CamelCaseStrategy implements EnumNamingStrategy {

        /**
         * @since 2.15
         */
        public final static CamelCaseStrategy INSTANCE
            = new CamelCaseStrategy();

        @Override
        public String translate(String input) {
            if (input == null) {
                return input;
            }

            int length = input.length();
            StringBuilder result = new StringBuilder(length * 2);
            int resultLength = 0;
            boolean wasPrevTranslated = false;
            for (int i = 0; i < length; i++) {
                char c = input.charAt(i);
                if (i > 0 || c != '_') {
                    if (Character.isUpperCase(c)) {
                        if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
                            result.append('_');
                            resultLength++;
                        }
                        c = Character.toLowerCase(c);
                        wasPrevTranslated = true;
                    } else {
                        wasPrevTranslated = false;
                    }
                    result.append(c);
                    resultLength++;
                }
            }
            String output = resultLength > 0 ? result.toString() : input;
            return output.toUpperCase();
        }
    }
}

package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;

/**
 * Class that defines how names of JSON properties ("external names")
 * are derived from names of POJO methods and fields ("internal names"),
 * in cases where no explicit annotations exist for naming.
 * Methods are passed information about POJO member for which name is needed,
 * as well as default name that would be used if no custom strategy was used.
 *<p>
 * Default (empty) implementation returns suggested ("implicit" or "default") name unmodified
 *<p>
 * Note that the strategy is guaranteed to be called once per logical property
 * (which may be represented by multiple members; such as pair of a getter and
 * a setter), but may be called for each: implementations should not count on
 * exact number of times, and should work for any member that represent a
 * property.
 * Also note that calls are made during construction of serializers and deserializers
 * which are typically cached, and not for every time serializer or deserializer
 * is called.
 *<p>
 * In absence of a registered custom strategy, the default Java property naming strategy
 * is used, which leaves field names as is, and removes set/get/is prefix
 * from methods (as well as lower-cases initial sequence of capitalized
 * characters).
 *<p>
 * NOTE! Since 2.12 sub-classes defined here (as well as static singleton instances thereof)
 * are deprecated due to
 * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>.
 * Please use constants and classes in {@link PropertyNamingStrategies} instead.
 *
 */
@SuppressWarnings("serial")
public class PropertyNamingStrategy // NOTE: was abstract until 2.7
    implements java.io.Serializable
{
    private static final long serialVersionUID = 2L;

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#LOWER_CAMEL_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy LOWER_CAMEL_CASE = new PropertyNamingStrategy();

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#UPPER_CAMEL_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy UPPER_CAMEL_CASE = new UpperCamelCaseStrategy();

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#SNAKE_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy SNAKE_CASE = new SnakeCaseStrategy();

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#LOWER_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy LOWER_CASE = new LowerCaseStrategy();

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#KEBAB_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy KEBAB_CASE = new KebabCaseStrategy();

    /**
     * @deprecated Since 2.12 deprecated. Use {@link PropertyNamingStrategies#LOWER_DOT_CASE} instead.
     * See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated // since 2.12
    public static final PropertyNamingStrategy LOWER_DOT_CASE = new LowerDotCaseStrategy();

    /*
    /**********************************************************
    /* API
    /**********************************************************
     */

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given field.
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param field Field used to access property
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the field represents
     */
    public String nameForField(MapperConfig<?> config, AnnotatedField field,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given getter method; typically called when building a serializer.
     * (but not always -- when using "getter-as-setter", may be called during
     * deserialization)
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param method Method used to access property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the method represents
     */
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given setter method; typically called when building a deserializer
     * (but not necessarily only then).
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param method Method used to access property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     *
     * @return Logical name to use for property that the method represents
     */
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method,
            String defaultName)
    {
        return defaultName;
    }

    /**
     * Method called to find external name (name used in JSON) for given logical
     * POJO property,
     * as defined by given constructor parameter; typically called when building a deserializer
     * (but not necessarily only then).
     *
     * @param config Configuration in used: either <code>SerializationConfig</code>
     *   or <code>DeserializationConfig</code>, depending on whether method is called
     *   during serialization or deserialization
     * @param ctorParam Constructor parameter used to pass property.
     * @param defaultName Default name that would be used for property in absence of custom strategy
     */
    public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam,
            String defaultName)
    {
        return defaultName;
    }

    /*
    /**********************************************************
    /* Public base class for simple implementations
    /**********************************************************
     */

    /**
     * @deprecated Since 2.12 deprecated. See
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reasons for deprecation.
     */
    @Deprecated
    public static abstract class PropertyNamingStrategyBase extends PropertyNamingStrategy
    {
        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName)
        {
            return translate(defaultName);
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName)
        {
            return translate(defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName)
        {
            return translate(defaultName);
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam,
                String defaultName)
        {
            return translate(defaultName);
        }

        public abstract String translate(String propertyName);

        /**
         * Helper method to share implementation between snake and dotted case.
         */
        protected static String translateLowerCaseWithSeparator(final String input, final char separator)
        {
            if (input == null) {
                return input; // garbage in, garbage out
            }
            final int length = input.length();
            if (length == 0) {
                return input;
            }

            final StringBuilder result = new StringBuilder(length + (length >> 1));
            int upperCount = 0;
            for (int i = 0; i < length; ++i) {
                char ch = input.charAt(i);
                char lc = Character.toLowerCase(ch);

                if (lc == ch) { // lower-case letter means we can get new word
                    // but need to check for multi-letter upper-case (acronym), where assumption
                    // is that the last upper-case char is start of a new word
                    if (upperCount > 1) {
                        // so insert hyphen before the last character now
                        result.insert(result.length() - 1, separator);
                    }
                    upperCount = 0;
                } else {
                    // Otherwise starts new word, unless beginning of string
                    if ((upperCount == 0) && (i > 0)) {
                        result.append(separator);
                    }
                    ++upperCount;
                }
                result.append(lc);
            }
            return result.toString();
        }
    }

    /*
    /**********************************************************
    /* Standard implementations
    /**********************************************************
     */

    /**
     * @deprecated Since 2.12 use {@link PropertyNamingStrategies.SnakeCaseStrategy} instead
     * (see
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reason for deprecation)
     */
    @Deprecated // since 2.12
    public static class SnakeCaseStrategy extends PropertyNamingStrategyBase
    {
        @Override
        public String translate(String input)
        {
            if (input == null) return input; // garbage in, garbage out
            int length = input.length();
            StringBuilder result = new StringBuilder(length * 2);
            int resultLength = 0;
            boolean wasPrevTranslated = false;
            for (int i = 0; i < length; i++)
            {
                char c = input.charAt(i);
                if (i > 0 || c != '_') // skip first starting underscore
                {
                    if (Character.isUpperCase(c))
                    {
                        if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_')
                        {
                            result.append('_');
                            resultLength++;
                        }
                        c = Character.toLowerCase(c);
                        wasPrevTranslated = true;
                    }
                    else
                    {
                        wasPrevTranslated = false;
                    }
                    result.append(c);
                    resultLength++;
                }
            }
            return resultLength > 0 ? result.toString() : input;
        }
    }

    /**
     * @deprecated Since 2.12 use {@link PropertyNamingStrategies.UpperCamelCaseStrategy} instead
     * (see
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reason for deprecation)
     */
    @Deprecated // since 2.12
    public static class UpperCamelCaseStrategy extends PropertyNamingStrategyBase
    {
        /**
         * Converts camelCase to PascalCase
         *
         * For example, "userName" would be converted to
         * "UserName".
         *
         * @param input formatted as camelCase string
         * @return input converted to PascalCase format
         */
        @Override
        public String translate(String input) {
            if (input == null || input.isEmpty()){
                return input; // garbage in, garbage out
            }
            // Replace first lower-case letter with upper-case equivalent
            char c = input.charAt(0);
            char uc = Character.toUpperCase(c);
            if (c == uc) {
                return input;
            }
            StringBuilder sb = new StringBuilder(input);
            sb.setCharAt(0, uc);
            return sb.toString();
        }
    }

    /**
     * @deprecated Since 2.12 use {@link PropertyNamingStrategies.LowerCaseStrategy} instead
     * (see
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reason for deprecation)
     */
    @Deprecated // since 2.12
    public static class LowerCaseStrategy extends PropertyNamingStrategyBase
    {
        @Override
        public String translate(String input) {
            return input.toLowerCase();
        }
    }

    /**
     * @deprecated Since 2.12 use {@link PropertyNamingStrategies.KebabCaseStrategy} instead
     * (see
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reason for deprecation)
     */
    @Deprecated // since 2.12
    public static class KebabCaseStrategy extends PropertyNamingStrategyBase
    {
        @Override
        public String translate(String input) {
            return translateLowerCaseWithSeparator(input, '-');
        }
    }

    /**
     * @deprecated Since 2.12 use {@link PropertyNamingStrategies.LowerDotCaseStrategy} instead
     * (see
     * <a href="https://github.com/FasterXML/jackson-databind/issues/2715">databind#2715</a>
     * for reason for deprecation)
     */
    @Deprecated // since 2.12
    public static class LowerDotCaseStrategy extends PropertyNamingStrategyBase {
        @Override
        public String translate(String input){
            return translateLowerCaseWithSeparator(input, '.');
        }
    }

    /*
    /**********************************************************
    /* Deprecated variants, aliases
    /**********************************************************
     */

    /**
     * @deprecated Since 2.7 use {@link PropertyNamingStrategies#SNAKE_CASE} instead.
     */
    @Deprecated // since 2.7
    public static final PropertyNamingStrategy CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES = SNAKE_CASE;

    /**
     * @deprecated Since 2.7 use {@link PropertyNamingStrategies#UPPER_CAMEL_CASE} instead;
     */
    @Deprecated // since 2.7
    public static final PropertyNamingStrategy PASCAL_CASE_TO_CAMEL_CASE = UPPER_CAMEL_CASE;

    /**
     * @deprecated In 2.7 use {@link PropertyNamingStrategies.SnakeCaseStrategy} instead
     */
    @Deprecated // since 2.7
    public static class LowerCaseWithUnderscoresStrategy extends SnakeCaseStrategy {}

    /**
     * @deprecated In 2.7 use {@link PropertyNamingStrategies.UpperCamelCaseStrategy} instead
     */
    @Deprecated // since 2.7
    public static class PascalCaseStrategy extends UpperCamelCaseStrategy { }
}

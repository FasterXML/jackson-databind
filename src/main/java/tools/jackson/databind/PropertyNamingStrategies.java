package tools.jackson.databind;

import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.util.NamingStrategyImpls;

/**
 * Container for standard {@link PropertyNamingStrategy} implementations
 * and singleton instances.
 */
public abstract class PropertyNamingStrategies
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Static instances that may be referenced
    /**********************************************************************
     */

    /**
     * Naming convention used in Java, where words other than first are capitalized
     * and no separator is used between words. Since this is the native Java naming convention,
     * naming strategy will not do any transformation between names in data (JSON) and
     * POJOS.
     *<p>
     * Example external property names would be "numberValue", "namingStrategy", "theDefiniteProof".
     */
    public static final PropertyNamingStrategy LOWER_CAMEL_CASE = new LowerCamelCaseStrategy();

    /**
     * Naming convention used in languages like Pascal, where all words are capitalized
     * and no separator is used between words.
     * See {@link UpperCamelCaseStrategy} for details.
     *<p>
     * Example external property names would be "NumberValue", "NamingStrategy", "TheDefiniteProof".
     */
    public static final PropertyNamingStrategy UPPER_CAMEL_CASE = new UpperCamelCaseStrategy();

    /**
     * Naming convention used in languages like C, where words are in lower-case
     * letters, separated by underscores.
     * See {@link SnakeCaseStrategy} for details.
     *<p>
     * Example external property names would be "number_value", "naming_strategy", "the_definite_proof".
     */
    public static final PropertyNamingStrategy SNAKE_CASE = new SnakeCaseStrategy();

    /**
     * Naming convention in which the words are in upper-case letters, separated by underscores.
     * See {@link UpperSnakeCaseStrategy} for details.
     */
    public static final PropertyNamingStrategy UPPER_SNAKE_CASE = new UpperSnakeCaseStrategy();

    /**
     * Naming convention in which all words of the logical name are in lower case, and
     * no separator is used between words.
     * See {@link LowerCaseStrategy} for details.
     *<p>
     * Example external property names would be "numbervalue", "namingstrategy", "thedefiniteproof".
     */
    public static final PropertyNamingStrategy LOWER_CASE = new LowerCaseStrategy();

    /**
     * Naming convention used in languages like Lisp, where words are in lower-case
     * letters, separated by hyphens.
     * See {@link KebabCaseStrategy} for details.
     *<p>
     * Example external property names would be "number-value", "naming-strategy", "the-definite-proof".
     */
    public static final PropertyNamingStrategy KEBAB_CASE = new KebabCaseStrategy();

    /**
     * Naming convention widely used as configuration properties name, where words are in
     * lower-case letters, separated by dots.
     * See {@link LowerDotCaseStrategy} for details.
     *<p>
     * Example external property names would be "number.value", "naming.strategy", "the.definite.proof".
     */
    public static final PropertyNamingStrategy LOWER_DOT_CASE = new LowerDotCaseStrategy();

    /*
    /**********************************************************************
    /* Public base class for simple implementations
    /**********************************************************************
     */

    /**
     * Intermediate base class for simple implementations
     */
    public static abstract class NamingBase
        extends PropertyNamingStrategy
    {
        private static final long serialVersionUID = 3L;

        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
            return translate(defaultName);
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return translate(defaultName);
        }

        @Override
        public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            return translate(defaultName);
        }

        @Override
        public String nameForConstructorParameter(MapperConfig<?> config, AnnotatedParameter ctorParam,
                String defaultName) {
            return translate(defaultName);
        }

        protected abstract String translate(String propertyName);
    }

    /*
    /**********************************************************************
    /* Standard implementations
    /**********************************************************************
     */

    /**
     * A {@link PropertyNamingStrategy} that translates typical camel case Java
     * property names to lower case JSON element names, separated by
     * underscores.  This implementation is somewhat lenient, in that it
     * provides some additional translations beyond strictly translating from
     * camel case only.  In particular, the following translations are applied
     * by this PropertyNamingStrategy.
     *
     * <ul><li>Every upper case letter in the Java property name is translated
     * into two characters, an underscore and the lower case equivalent of the
     * target character, with three exceptions.
     * <ol><li>For contiguous sequences of upper case letters, characters after
     * the first character are replaced only by their lower case equivalent,
     * and are not preceded by an underscore.
     * <ul><li>This provides for reasonable translations of upper case acronyms,
     * e.g., &quot;theWWW&quot; is translated to &quot;the_www&quot;.</li></ul></li>
     * <li>An upper case character in the first position of the Java property
     * name is not preceded by an underscore character, and is translated only
     * to its lower case equivalent.
     * <ul><li>For example, &quot;Results&quot; is translated to &quot;results&quot;,
     * and not to &quot;_results&quot;.</li></ul></li>
     * <li>An upper case character in the Java property name that is already
     * preceded by an underscore character is translated only to its lower case
     * equivalent, and is not preceded by an additional underscore.
     * <ul><li>For example, &quot;user_Name&quot; is translated to
     * &quot;user_name&quot;, and not to &quot;user__name&quot; (with two
     * underscore characters).</li></ul></li></ol></li>
     * <li>If the Java property name starts with an underscore, then that
     * underscore is not included in the translated name, unless the Java
     * property name is just one character in length, i.e., it is the
     * underscore character.  This applies only to the first character of the
     * Java property name.</li></ul>
     *<p>
     * These rules result in the following additional example translations from
     * Java property names to JSON element names.
     * <ul><li>&quot;userName&quot; is translated to &quot;user_name&quot;</li>
     * <li>&quot;UserName&quot; is translated to &quot;user_name&quot;</li>
     * <li>&quot;USER_NAME&quot; is translated to &quot;user_name&quot;</li>
     * <li>&quot;user_name&quot; is translated to &quot;user_name&quot; (unchanged)</li>
     * <li>&quot;user&quot; is translated to &quot;user&quot; (unchanged)</li>
     * <li>&quot;User&quot; is translated to &quot;user&quot;</li>
     * <li>&quot;USER&quot; is translated to &quot;user&quot;</li>
     * <li>&quot;_user&quot; is translated to &quot;user&quot;</li>
     * <li>&quot;_User&quot; is translated to &quot;user&quot;</li>
     * <li>&quot;__user&quot; is translated to &quot;_user&quot;
     * (the first of two underscores was removed)</li>
     * <li>&quot;user__name&quot; is translated to &quot;user__name&quot;
     * (unchanged, with two underscores)</li></ul>
     */
    public static class SnakeCaseStrategy extends NamingBase
    {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input)
        {
            return NamingStrategyImpls.SNAKE_CASE.translate(input);
        }
    }

    /**
     * A {@link PropertyNamingStrategy} that translates an input to the equivalent upper case snake
     * case. The class extends {@link PropertyNamingStrategies.SnakeCaseStrategy} to retain the
     * snake case conversion functionality offered by the strategy.
     */
    public static class UpperSnakeCaseStrategy extends SnakeCaseStrategy
    {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input) {
            return NamingStrategyImpls.UPPER_SNAKE_CASE.translate(input);
        }
    }

    /**
     * "No-operation" strategy that is equivalent to not specifying any
     * strategy: will simply return suggested standard bean naming as-is.
     */
    public static class LowerCamelCaseStrategy extends NamingBase
    {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input) {
            return NamingStrategyImpls.LOWER_CAMEL_CASE.translate(input);
        }
    }

    /**
     * A {@link PropertyNamingStrategy} that translates typical camelCase Java
     * property names to PascalCase JSON element names (i.e., with a capital
     * first letter).  In particular, the following translations are applied by
     * this PropertyNamingStrategy.
     *
     * <ul><li>The first lower-case letter in the Java property name is translated
     * into its equivalent upper-case representation.</li></ul>
     *
     * This rules result in the following example translation from
     * Java property names to JSON element names.
     * <ul><li>&quot;userName&quot; is translated to &quot;UserName&quot;</li></ul>
     */
    public static class UpperCamelCaseStrategy extends NamingBase
    {
        private static final long serialVersionUID = 3L;

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
        protected String translate(String input) {
            return NamingStrategyImpls.UPPER_CAMEL_CASE.translate(input);
        }
    }

    /**
     * Simple strategy where external name simply only uses lower-case characters,
     * and no separators.
     * Conversion from internal name like "someOtherValue" would be into external name
     * if "someothervalue".
     */
    public static class LowerCaseStrategy extends NamingBase
    {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input) {
            return NamingStrategyImpls.LOWER_CASE.translate(input);
        }
    }

    /**
     * Naming strategy similar to {@link PropertyNamingStrategies.SnakeCaseStrategy},
     * but instead of underscores
     * as separators, uses hyphens. Naming convention traditionally used for languages
     * like Lisp.
     */
    public static class KebabCaseStrategy extends NamingBase
    {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input) {
            return NamingStrategyImpls.KEBAB_CASE.translate(input);
        }
    }

    /**
     * Naming strategy similar to {@link PropertyNamingStrategies.KebabCaseStrategy},
     * but instead of hyphens
     * as separators, uses dots. Naming convention widely used as configuration properties name.
     */
    public static class LowerDotCaseStrategy extends NamingBase {
        private static final long serialVersionUID = 3L;

        @Override
        protected String translate(String input){
            return NamingStrategyImpls.LOWER_DOT_CASE.translate(input);
        }
    }
}

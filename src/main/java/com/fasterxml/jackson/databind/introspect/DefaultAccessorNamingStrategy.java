package com.fasterxml.jackson.databind.introspect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jdk14.JDK14Util;

/**
 * Default {@link AccessorNamingStrategy} used by Jackson: to be used either as-is,
 * or as base-class with overrides.
 *
 * @since 2.12
 */
public class DefaultAccessorNamingStrategy
    extends AccessorNamingStrategy
{
    /**
     * Definition of a handler API to use for checking whether given base name
     * (remainder of accessor method name after removing prefix) is acceptable
     * based on various rules.
     *
     * @since 2.12
     */
    public interface BaseNameValidator {
        public boolean accept(char firstChar, String basename, int offset);
    }

    protected final MapperConfig<?> _config;
    protected final AnnotatedClass _forClass;

    /**
     * Optional validator for checking that base name
     */
    protected final BaseNameValidator _baseNameValidator;

    protected final boolean _stdBeanNaming;
    protected final boolean _isGettersNonBoolean;

    protected final String _getterPrefix;

    /**
     * @since 2.14
     */
    protected final String _isGetterPrefix;

    /**
     * Prefix used by auto-detected mutators ("setters"): usually "set",
     * but differs for builder objects ("with" by default).
     */
    protected final String _mutatorPrefix;

    protected DefaultAccessorNamingStrategy(MapperConfig<?> config, AnnotatedClass forClass,
            String mutatorPrefix, String getterPrefix, String isGetterPrefix,
            BaseNameValidator baseNameValidator)
    {
        _config = config;
        _forClass = forClass;

        _stdBeanNaming = config.isEnabled(MapperFeature.USE_STD_BEAN_NAMING);
        _isGettersNonBoolean = config.isEnabled(MapperFeature.ALLOW_IS_GETTERS_FOR_NON_BOOLEAN);
        _mutatorPrefix = mutatorPrefix;
        _getterPrefix = getterPrefix;
        _isGetterPrefix = isGetterPrefix;
        _baseNameValidator = baseNameValidator;
    }

    @Override
    public String findNameForIsGetter(AnnotatedMethod am, String name)
    {
        if (_isGetterPrefix != null) {
            final Class<?> rt = am.getRawType();
            if (_isGettersNonBoolean || rt == Boolean.class || rt == Boolean.TYPE) {
                if (name.startsWith(_isGetterPrefix)) {
                    return _stdBeanNaming
                            ? stdManglePropertyName(name, 2)
                            : legacyManglePropertyName(name, 2);
                }
            }
        }
        return null;
    }

    @Override
    public String findNameForRegularGetter(AnnotatedMethod am, String name)
    {
        if ((_getterPrefix != null) && name.startsWith(_getterPrefix)) {
            // 16-Feb-2009, tatu: To handle [JACKSON-53], need to block CGLib-provided
            // method "getCallbacks". Not sure of exact safe criteria to get decent
            // coverage without false matches; but for now let's assume there is
            // no reason to use any such getter from CGLib.
            if ("getCallbacks".equals(name)) {
                if (_isCglibGetCallbacks(am)) {
                    return null;
                }
            } else if ("getMetaClass".equals(name)) {
                // 30-Apr-2009, tatu: Need to suppress serialization of a cyclic reference
                if (_isGroovyMetaClassGetter(am)) {
                    return null;
                }
            }
            return _stdBeanNaming
                    ? stdManglePropertyName(name, _getterPrefix.length())
                    : legacyManglePropertyName(name, _getterPrefix.length());
        }
        return null;
    }

    @Override
    public String findNameForMutator(AnnotatedMethod am, String name)
    {
        if ((_mutatorPrefix != null) && name.startsWith(_mutatorPrefix)) {
            return _stdBeanNaming
                    ? stdManglePropertyName(name, _mutatorPrefix.length())
                    : legacyManglePropertyName(name, _mutatorPrefix.length());
        }
        return null;
    }

    // Default implementation simply returns name as-is
    @Override
    public String modifyFieldName(AnnotatedField field, String name) {
        return name;
    }

    /*
    /**********************************************************************
    /* Name-mangling methods copied in 2.12 from "BeanUtil"
    /**********************************************************************
     */

    /**
     * Method called to figure out name of the property, given
     * corresponding suggested name based on a method or field name.
     *
     * @param basename Name of accessor/mutator method, not including prefix
     *  ("get"/"is"/"set")
     */
    protected String legacyManglePropertyName(final String basename, final int offset)
    {
        final int end = basename.length();
        if (end == offset) { // empty name, nope
            return null;
        }
        char c = basename.charAt(offset);
        // 12-Oct-2020, tatu: Additional configurability; allow checking that
        //    base name is acceptable (currently just by checking first character)
        if (_baseNameValidator != null) {
            if (!_baseNameValidator.accept(c, basename, offset)) {
                return null;
            }
        }

        // next check: is the first character upper case? If not, return as is
        char d = Character.toLowerCase(c);

        if (c == d) {
            return basename.substring(offset);
        }
        // otherwise, lower case initial chars. Common case first, just one char
        StringBuilder sb = new StringBuilder(end - offset);
        sb.append(d);
        int i = offset+1;
        for (; i < end; ++i) {
            c = basename.charAt(i);
            d = Character.toLowerCase(c);
            if (c == d) {
                sb.append(basename, i, end);
                break;
            }
            sb.append(d);
        }
        return sb.toString();
    }

    protected String stdManglePropertyName(final String basename, final int offset)
    {
        final int end = basename.length();
        if (end == offset) { // empty name, nope
            return null;
        }
        // first: if it doesn't start with capital, return as-is
        char c0 = basename.charAt(offset);
        // 12-Oct-2020, tatu: Additional configurability; allow checking that
        //    base name is acceptable (currently just by checking first character)
        if (_baseNameValidator != null) {
            if (!_baseNameValidator.accept(c0, basename, offset)) {
                return null;
            }
        }

        char c1 = Character.toLowerCase(c0);
        if (c0 == c1) {
            return basename.substring(offset);
        }
        // 17-Dec-2014, tatu: As per [databind#653], need to follow more
        //   closely Java Beans spec; specifically, if two first are upper-case,
        //   then no lower-casing should be done.
        if ((offset + 1) < end) {
            if (Character.isUpperCase(basename.charAt(offset+1))) {
                return basename.substring(offset);
            }
        }
        StringBuilder sb = new StringBuilder(end - offset);
        sb.append(c1);
        sb.append(basename, offset+1, end);
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Legacy methods moved in 2.12 from "BeanUtil" -- are these still needed?
    /**********************************************************************
     */

    // This method was added to address the need to weed out CGLib-injected
    // "getCallbacks" method.
    // At this point caller has detected a potential getter method with
    // name "getCallbacks" and we need to determine if it is indeed injected
    // by Cglib. We do this by verifying that the  result type is "net.sf.cglib.proxy.Callback[]"
    protected boolean _isCglibGetCallbacks(AnnotatedMethod am)
    {
        Class<?> rt = am.getRawType();
        // Ok, first: must return an array type
        if (rt.isArray()) {
            // And that type needs to be "net.sf.cglib.proxy.Callback".
            // Theoretically could just be a type that implements it, but
            // for now let's keep things simple, fix if need be.

            Class<?> compType = rt.getComponentType();
            // Actually, let's just verify it's a "net.sf.cglib.*" class/interface
            final String className = compType.getName();
            if (className.contains(".cglib")) {
                return className.startsWith("net.sf.cglib")
                    // also, as per [JACKSON-177]
                    || className.startsWith("org.hibernate.repackage.cglib")
                    // and [core#674]
                    || className.startsWith("org.springframework.cglib");
            }
        }
        return false;
    }

    // Another helper method to deal with Groovy's problematic metadata accessors
    protected boolean _isGroovyMetaClassGetter(AnnotatedMethod am) {
        return am.getRawType().getName().startsWith("groovy.lang");
    }

    /*
    /**********************************************************************
    /* Standard Provider implementation
    /**********************************************************************
     */

    /**
     * Provider for {@link DefaultAccessorNamingStrategy}.
     *<p>
     * Default instance will use following default prefixes:
     *<ul>
     * <li>Setter for regular POJOs: "set"
     *  </li>
     * <li>Builder-mutator: "with"
     *  </li>
     * <li>Regular getter: "get"
     *  </li>
     * <li>Is-getter (for Boolean values): "is"
     *  </li>
     *</ul>
     * and no additional restrictions on base names accepted (configurable for
     * limits using {@link BaseNameValidator}), allowing names like
     * "get_value()" and "getvalue()".
     */
    public static class Provider
        extends AccessorNamingStrategy.Provider
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 1L;

        protected final String _setterPrefix;
        protected final String _withPrefix;

        protected final String _getterPrefix;
        protected final String _isGetterPrefix;

        protected final BaseNameValidator _baseNameValidator;

        public Provider() {
            this("set", JsonPOJOBuilder.DEFAULT_WITH_PREFIX,
                    "get", "is", null);
        }

        protected Provider(Provider p,
                String setterPrefix, String withPrefix,
                String getterPrefix, String isGetterPrefix)
        {
            this(setterPrefix, withPrefix, getterPrefix, isGetterPrefix,
                    p._baseNameValidator);
        }

        protected Provider(Provider p, BaseNameValidator vld)
        {
            this(p._setterPrefix, p._withPrefix,
                    p._getterPrefix, p._isGetterPrefix, vld);
        }

        protected Provider(String setterPrefix, String withPrefix,
                String getterPrefix, String isGetterPrefix,
                BaseNameValidator vld)
        {
            _setterPrefix = setterPrefix;
            _withPrefix = withPrefix;
            _getterPrefix = getterPrefix;
            _isGetterPrefix = isGetterPrefix;
            _baseNameValidator = vld;
        }

        /**
         * Mutant factory for changing the prefix used for "setter"
         * methods
         *
         * @param prefix Prefix to use; or empty String {@code ""} to not use
         *   any prefix (meaning signature-compatible method name is used as
         *   the property basename (and subject to name mangling)),
         *   or {@code null} to prevent name-based detection.
         *
         * @return Provider instance with specified setter-prefix
         */
        public Provider withSetterPrefix(String prefix) {
            return new Provider(this,
                    prefix, _withPrefix, _getterPrefix, _isGetterPrefix);
        }

        /**
         * Mutant factory for changing the prefix used for Builders
         * (from default {@link JsonPOJOBuilder#DEFAULT_WITH_PREFIX})
         *
         * @param prefix Prefix to use; or empty String {@code ""} to not use
         *   any prefix (meaning signature-compatible method name is used as
         *   the property basename (and subject to name mangling)),
         *   or {@code null} to prevent name-based detection.
         *
         * @return Provider instance with specified with-prefix
         */
        public Provider withBuilderPrefix(String prefix) {
            return new Provider(this,
                    _setterPrefix, prefix, _getterPrefix, _isGetterPrefix);
        }

        /**
         * Mutant factory for changing the prefix used for "getter"
         * methods
         *
         * @param prefix Prefix to use; or empty String {@code ""} to not use
         *   any prefix (meaning signature-compatible method name is used as
         *   the property basename (and subject to name mangling)),
         *   or {@code null} to prevent name-based detection.
         *
         * @return Provider instance with specified getter-prefix
         */
        public Provider withGetterPrefix(String prefix) {
            return new Provider(this,
                    _setterPrefix, _withPrefix, prefix, _isGetterPrefix);
        }

        /**
         * Mutant factory for changing the prefix used for "is-getter"
         * methods (getters that return boolean/Boolean value).
         *
         * @param prefix Prefix to use; or empty String {@code ""} to not use
         *   any prefix (meaning signature-compatible method name is used as
         *   the property basename (and subject to name mangling)).
         *   or {@code null} to prevent name-based detection.
         *
         * @return Provider instance with specified is-getter-prefix
         */
        public Provider withIsGetterPrefix(String prefix) {
            return new Provider(this,
                    _setterPrefix, _withPrefix, _getterPrefix, prefix);
        }

        /**
         * Mutant factory for changing the rules regarding which characters
         * are allowed as the first character of property base name, after
         * checking and removing prefix.
         *<p>
         * For example, consider "getter" method candidate (no arguments, has return
         * type) named {@code getValue()} is considered, with "getter-prefix"
         * defined as {@code get}, then base name is {@code Value} and the
         * first character to consider is {@code V}. Upper-case letters are
         * always accepted so this is fine.
         * But with similar settings, method {@code get_value()} would only be
         * recognized as getter if {@code allowNonLetterFirstChar} is set to
         * {@code true}: otherwise it will not be considered a getter-method.
         * Similarly "is-getter" candidate method with name {@code island()}
         * would only be considered if {@code allowLowerCaseFirstChar} is set
         * to {@code true}.
         *
         * @param allowLowerCaseFirstChar Whether base names that start with lower-case
         *    letter (like {@code "a"} or {@code "b"}) are accepted as valid or not:
         *    consider difference between "setter-methods" {@code setValue()} and {@code setvalue()}.
         * @param allowNonLetterFirstChar  Whether base names that start with non-letter
         *    character (like {@code "_"} or number {@code 1}) are accepted as valid or not:
         *    consider difference between "setter-methods" {@code setValue()} and {@code set_value()}.
         *
         * @return Provider instance with specified validity rules
         */
        public Provider withFirstCharAcceptance(boolean allowLowerCaseFirstChar,
                boolean allowNonLetterFirstChar) {
            return withBaseNameValidator(
                    FirstCharBasedValidator.forFirstNameRule(allowLowerCaseFirstChar, allowNonLetterFirstChar));
        }

        /**
         * Mutant factory for specifying validator that is used to further verify that
         * base name derived from accessor name is acceptable: this can be used to add
         * further restrictions such as limit that the first character of the base name
         * is an upper-case letter.
         *
         * @param vld Validator to use, if any; {@code null} to indicate no additional rules
         *
         * @return Provider instance with specified base name validator to use, if any
         */
        public Provider withBaseNameValidator(BaseNameValidator vld) {
            return new Provider(this, vld);
        }

        @Override
        public AccessorNamingStrategy forPOJO(MapperConfig<?> config, AnnotatedClass targetClass)
        {
            return new DefaultAccessorNamingStrategy(config, targetClass,
                    _setterPrefix, _getterPrefix, _isGetterPrefix,
                    _baseNameValidator);
        }

        @Override
        public AccessorNamingStrategy forBuilder(MapperConfig<?> config,
                AnnotatedClass builderClass, BeanDescription valueTypeDesc)
        {
            AnnotationIntrospector ai = config.isAnnotationProcessingEnabled() ? config.getAnnotationIntrospector() : null;
            JsonPOJOBuilder.Value builderConfig = (ai == null) ? null : ai.findPOJOBuilderConfig(builderClass);
            String mutatorPrefix = (builderConfig == null) ? _withPrefix : builderConfig.withPrefix;
            return new DefaultAccessorNamingStrategy(config, builderClass,
                    mutatorPrefix, _getterPrefix, _isGetterPrefix,
                    _baseNameValidator);
        }

        @Override
        public AccessorNamingStrategy forRecord(MapperConfig<?> config, AnnotatedClass recordClass)
        {
            return new RecordNaming(config, recordClass);
        }
    }

    /**
     * Simple implementation of {@link BaseNameValidator} that checks the
     * first character and nothing else.
     *<p>
     * Instances are to be constructed using method
     * {@link FirstCharBasedValidator#forFirstNameRule}.
     */
    public static class FirstCharBasedValidator
        implements BaseNameValidator
    {
        private final boolean _allowLowerCaseFirstChar;
        private final boolean _allowNonLetterFirstChar;

        protected FirstCharBasedValidator(boolean allowLowerCaseFirstChar,
                boolean allowNonLetterFirstChar) {
            _allowLowerCaseFirstChar = allowLowerCaseFirstChar;
            _allowNonLetterFirstChar = allowNonLetterFirstChar;
        }

        /**
         * Factory method to use for getting an instance with specified first-character
         * restrictions, if any; or {@code null} if no checking is needed.
         *
         * @param allowLowerCaseFirstChar Whether base names that start with lower-case
         *    letter (like {@code "a"} or {@code "b"}) are accepted as valid or not:
         *    consider difference between "setter-methods" {@code setValue()} and {@code setvalue()}.
         * @param allowNonLetterFirstChar  Whether base names that start with non-letter
         *    character (like {@code "_"} or number {@code 1}) are accepted as valid or not:
         *    consider difference between "setter-methods" {@code setValue()} and {@code set_value()}.
         *
         * @return Validator instance to use, if any; {@code null} to indicate no additional
         *   rules applied (case when both arguments are {@code false})
         */
        public static BaseNameValidator forFirstNameRule(boolean allowLowerCaseFirstChar,
                boolean allowNonLetterFirstChar) {
            if (!allowLowerCaseFirstChar && !allowNonLetterFirstChar) {
                return null;
            }
            return new FirstCharBasedValidator(allowLowerCaseFirstChar,
                    allowNonLetterFirstChar);
        }

        @Override
        public boolean accept(char firstChar, String basename, int offset) {
            // Ok, so... If UTF-16 letter, then check whether lc allowed
            // (title-case and upper-case both assumed to be acceptable by default)
            if (Character.isLetter(firstChar)) {
                return _allowLowerCaseFirstChar || !Character.isLowerCase(firstChar);
            }
            // Otherwise, non-letter checking applied
            return _allowNonLetterFirstChar;
        }
    }

    /**
     * Implementation used for supporting "non-prefix" naming convention of
     * Java 14 {@code java.lang.Record} types, and in particular find default
     * accessors for declared record fields.
     *<p>
     * Current / initial implementation will also recognize additional "normal"
     * getters ("get"-prefix) and is-getters ("is"-prefix and boolean return value)
     * by name.
     */
    public static class RecordNaming
        extends DefaultAccessorNamingStrategy
    {
        /**
         * Names of actual Record fields from definition; auto-detected.
         */
        protected final Set<String> _fieldNames;

        public RecordNaming(MapperConfig<?> config, AnnotatedClass forClass) {
            super(config, forClass,
                    // no setters for (immutable) Records:
                    null,
                    // trickier: regular fields are ok (handled differently), but should
                    // we also allow getter discovery? For now let's do so
                    "get", "is", null);
            String[] recordFieldNames = JDK14Util.getRecordFieldNames(forClass.getRawType());
            // 01-May-2022, tatu: Due to [databind#3417] may return null when no info available
            _fieldNames = recordFieldNames == null ?
                    Collections.emptySet() :
                    new HashSet<>(Arrays.asList(recordFieldNames));
        }

        @Override
        public String findNameForRegularGetter(AnnotatedMethod am, String name)
        {
            // By default, field names are un-prefixed, but verify so that we will not
            // include "toString()" or additional custom methods (unless latter are
            // annotated for inclusion)
            if (_fieldNames.contains(name)) {
                return name;
            }
            // but also allow auto-detecting additional getters, if any?
            return super.findNameForRegularGetter(am, name);
        }
    }
}

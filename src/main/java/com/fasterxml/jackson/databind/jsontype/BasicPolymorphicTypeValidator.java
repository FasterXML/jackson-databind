package com.fasterxml.jackson.databind.jsontype;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InsecureTypeMatchException;

/**
 * Standard {@link BasicPolymorphicTypeValidator} implementation that users may want
 * to use for constructing validators based on simple class hierarchy and/or name patterns
 * to allow and/or deny certain subtypes.
 *<p>
 * Most commonly this is used to allow known safe subtypes based on common super type
 * or Java package name.
 *<br>
 * For example:
 *<pre>
 *</pre>
 *
 * @since 2.10
 */
public class BasicPolymorphicTypeValidator
    extends PolymorphicTypeValidator.Base
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Helper classes: matchers
    /**********************************************************
     */

    /**
     * General matcher interface (predicate) for validating class values
     * (base type or resolved subtype)
     */
    protected abstract static class TypeMatcher {
        public abstract boolean match(Class<?> clazz);
    }

    /**
     * General matcher interface (predicate) for validating unresolved
     * subclass class name.
     */
    protected abstract static class NameMatcher {
        public abstract boolean match(String clazzName);
    }
    
    /*
    /**********************************************************
    /* Builder class for configuring instances
    /**********************************************************
     */

    /**
     * Builder class for configuring and constructing immutable
     * {@link BasicPolymorphicTypeValidator} instances. Criteria for allowing
     * polymorphic subtypes is specified by adding rules in priority order, starting
     * with the rules to evaluate first: when a matching rule is found, its status
     * ({@link PolymorphicTypeValidator.Validity#ALLOWED} or {@link PolymorphicTypeValidator.Validity#DENIED}) is used and no further
     * rules are checked.
     */
    public static class Builder {

        private static final List<Class<?>> invalidBaseClasses = Arrays.asList(Serializable.class,Object.class);


        protected Set<Class<?>> _invalidBaseTypes;

        /**
         * Collected matchers for base types to allow.
         */
        protected List<TypeMatcher> _baseTypeMatchers;

        /**
         * Collected name-based matchers for sub types to allow.
         */
        protected List<NameMatcher> _subTypeNameMatchers;

        /**
         * Collected Class-based matchers for sub types to allow.
         */
        protected List<TypeMatcher> _subTypeClassMatchers;

        
        protected Builder() { }



        // // Methods for checking solely by base type (before subtype even considered)

        /**
         * Method for appending matcher that will allow all subtypes in cases where
         * nominal base type is specified class, or one of its subtypes.
         * For example, call to
         *<pre>
         *    builder.allowIfBaseType(MyBaseType.class)
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is {@code MyBaseType} (or subclass thereof) would allow all legal (assignment-compatible)
         * subtypes.
         */
        public Builder allowIfBaseType(final Class<?> baseOfBase) {
            return _appendBaseMatcher(new TypeMatcher() {
                @Override
                public boolean match(Class<?> clazz) {
                    return baseOfBase.isAssignableFrom(clazz);
                }
            });
        }

        /**
         * Method for appending matcher that will allow all subtypes in cases where
         * nominal base type's class name matches given {@link Pattern}
         * For example, call to
         *<pre>
         *    builder.allowIfBaseType(Pattern.compile("com\\.mycompany\\..*")
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is in package {@code com.mycompany} would allow all legal (assignment-compatible)
         * subtypes.
         *<p>
         * NOTE! {@link Pattern} match is applied using
         *<code>
         *   if (patternForBase.matcher(typeId).matches()) { }
         *</code>
         * that is, it must match the whole class name, not just part.
         */
        public Builder allowIfBaseType(final Pattern patternForBase) {
            return _appendBaseMatcher(new TypeMatcher() {
                @Override
                public boolean match(Class<?> clazz) {
                    return patternForBase.matcher(clazz.getName()).matches();
                }
            });
        }

        /**
         * Method for appending matcher that will allow all subtypes in cases where
         * nominal base type's class name starts with specific prefix.
         * For example, call to
         *<pre>
         *    builder.allowIfBaseType("com.mycompany.")
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is in package {@code com.mycompany} would allow all legal (assignment-compatible)
         * subtypes.
         */
        public Builder allowIfBaseType(final String prefixForBase) {
            return _appendBaseMatcher(new TypeMatcher() {
                @Override
                public boolean match(Class<?> clazz) {
                    return clazz.getName().startsWith(prefixForBase);
                }
            });
        }

        /**
         * Method for appending matcher that will mark any polymorphic properties with exact
         * specific class to be invalid.
         * For example, call to
         *<pre>
         *    builder.denyforExactBaseType(Object.class)
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is {@code java.lang.Object}
         * would be deemed invalid, and attempt to deserialize values of such types
         * should result in an exception.
         */
        public Builder denyForExactBaseType(final Class<?> baseTypeToDeny) {
            if (_invalidBaseTypes == null) {
                _invalidBaseTypes = new HashSet<>();
            }
            _invalidBaseTypes.add(baseTypeToDeny);
            return this;
        }

        // // Methods for considering subtype (base type was not enough)

        /**
         * Method for appending matcher that will allow specific subtype (regardless
         * of declared base type) if it is {@code subTypeBase} or its subtype.
         * For example, call to
         *<pre>
         *    builder.allowIfSubType(MyImplType.class)
         *</pre>
         * would indicate that any polymorphic values with type of
         * is {@code MyImplType} (or subclass thereof)
         * would be allowed.
         */
        public Builder allowIfSubType(final Class<?> subTypeBase) {
            return _appendSubClassMatcher(new TypeMatcher() {
                @Override
                public boolean match(Class<?> clazz) {
                    return subTypeBase.isAssignableFrom(clazz);
                }
            });
        }

        /**
         * Method for appending matcher that will allow specific subtype (regardless
         * of declared base type) in cases where subclass name matches given {@link Pattern}.
         * For example, call to
         *<pre>
         *    builder.allowIfSubType(Pattern.compile("com\\.mycompany\\.")
         *</pre>
         * would indicate that any polymorphic values in package {@code com.mycompany}
         * would be allowed.
         *<p>
         * NOTE! {@link Pattern} match is applied using
         *<code>
         *   if (patternForSubType.matcher(typeId).matches()) { }
         *</code>
         * that is, it must match the whole class name, not just part.
         */
        public Builder allowIfSubType(final Pattern patternForSubType) {
            return _appendSubNameMatcher(new NameMatcher() {
                @Override
                public boolean match(String clazzName) {
                    return patternForSubType.matcher(clazzName).matches();
                }
            });
        }

        /**
         * Method for appending matcher that will allow specific subtype (regardless
         * of declared base type)
         * in cases where subclass name starts with specified prefix
         * For example, call to
         *<pre>
         *    builder.allowIfSubType("com.mycompany.")
         *</pre>
         * would indicate that any polymorphic values in package {@code com.mycompany}
         * would be allowed.
         */
        public Builder allowIfSubType(final String prefixForSubType) {
            return _appendSubNameMatcher(new NameMatcher() {
                @Override
                public boolean match(String clazzName) {
                    return clazzName.startsWith(prefixForSubType);
                }
            });
        }

        /**
         * Validates and Constructs the BasicPolymorphicTypeValidator.
         * Unlike build() no check is done to ensure the Matchers added to this validator will allow Object.class
         * or Serializable.class and their subtypes to be deserialised.
         * As the name suggests, this is an insecure operation. Please think very carefully before calling this method
         * rather than build(), especially if the caller is outside the trust boundary of this application. As doing
         * so will leave your application open to vulnerabilities including remote code execution.
         * @return the BasicPolymorphicTypeValidator.
         */
        public BasicPolymorphicTypeValidator build_insecure()  {
            return new BasicPolymorphicTypeValidator(_invalidBaseTypes,
                    (_baseTypeMatchers == null) ? null : _baseTypeMatchers.toArray(new TypeMatcher[0]),
                    (_subTypeNameMatchers == null) ? null : _subTypeNameMatchers.toArray(new NameMatcher[0]),
                    (_subTypeClassMatchers == null) ? null : _subTypeClassMatchers.toArray(new TypeMatcher[0])
            );
        }

        /**
         * Validates and Constructs the BasicPolymorphicTypeValidator.
         * If a Matcher added to this Validator will allow Object.class or Serializable.class and all of their sub types
         * to be deserialised a InsecureTypeMatchException will be thrown.
         * This is due to the inherent security problems of allowing either all classes on the class path to be
         * arbitrarily deserialised in the case of Object.class or large amounts of classes in the case of
         * Serializable.class.
         * If you really need to allow either of these blocked classes. Use build_insecure(). However please be aware
         * of the very real security problems you are introducing into your application by doing so.
         * @return the BasicPolymorphicTypeValidator.
         * @throws InsecureTypeMatchException if any matcher added will allow Object.class, Serializable.class
         */
        public BasicPolymorphicTypeValidator build() throws InsecureTypeMatchException {
            validateTypeMatch();
            return build_insecure();
        }

        /**
         * validates that each of the Matchers will not allow Serializable.class or Object.class.
         * @throws InsecureTypeMatchException if an included Matcher will allow one or both of these classes through.
         */
        private void validateTypeMatch() throws InsecureTypeMatchException {
            if(_baseTypeMatchers!=null) {
                for (TypeMatcher tm : _baseTypeMatchers) {
                    for (Class<?> klass : invalidBaseClasses) {
                        if (tm.match(klass)) {
                            throw new InsecureTypeMatchException("Insecure Base class found : " + klass.getName());
                        }
                    }
                }
            }
            if(_subTypeNameMatchers!=null) {
                for (NameMatcher nm : _subTypeNameMatchers) {
                    for (Class<?> klass : invalidBaseClasses) {
                        if (nm.match(klass.getName())) {
                            throw new InsecureTypeMatchException("Insecure Base class found : " + klass.getName());
                        }
                    }
                }
            }
            if(_subTypeClassMatchers!=null) {
                for (TypeMatcher tm : _subTypeClassMatchers) {
                    for (Class<?> klass : invalidBaseClasses) {
                        if (tm.match(klass)) {
                            throw new InsecureTypeMatchException("Insecure Base class found : " + klass.getName());
                        }
                    }
                }
            }

        }

        protected Builder _appendBaseMatcher(TypeMatcher matcher) {
            if (_baseTypeMatchers == null) {
                _baseTypeMatchers = new ArrayList<>();
            }
            _baseTypeMatchers.add(matcher);
            return this;
        }

        protected Builder _appendSubNameMatcher(NameMatcher matcher) {
            if (_subTypeNameMatchers == null) {
                _subTypeNameMatchers = new ArrayList<>();
            }
            _subTypeNameMatchers.add(matcher);
            return this;
        }

        protected Builder _appendSubClassMatcher(TypeMatcher matcher) {
            if (_subTypeClassMatchers == null) {
                _subTypeClassMatchers = new ArrayList<>();
            }
            _subTypeClassMatchers.add(matcher);
            return this;
        }
    }

    /*
    /**********************************************************
    /* Actual implementation
    /**********************************************************
     */

    /**
     * Set of specifically denied base types to indicate that use of specific
     * base types is not allowed: most commonly used to fully block use of
     * {@link java.lang.Object} as the base type.
     */
    protected final Set<Class<?>> _invalidBaseTypes;

    /**
     * Set of matchers that can validate all values of polymorphic properties
     * that match specified allowed base types.
     */
    protected final TypeMatcher[] _baseTypeMatchers;

    /**
     * Set of matchers that can validate specific values of polymorphic properties
     * that match subtype class name criteria.
     */
    protected final NameMatcher[] _subTypeNameMatchers;

    /**
     * Set of matchers that can validate specific values of polymorphic properties
     * that match subtype class criteria.
     */
    protected final TypeMatcher[] _subClassMatchers;
    
    protected BasicPolymorphicTypeValidator(Set<Class<?>> invalidBaseTypes,
            TypeMatcher[] baseTypeMatchers,
            NameMatcher[] subTypeNameMatchers, TypeMatcher[] subClassMatchers) {
        _invalidBaseTypes = invalidBaseTypes;
        _baseTypeMatchers = baseTypeMatchers;
        _subTypeNameMatchers = subTypeNameMatchers;
        _subClassMatchers = subClassMatchers;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
        final Class<?> rawBase = baseType.getRawClass();
        if (_invalidBaseTypes != null) {
            if (_invalidBaseTypes.contains(rawBase)) {
                return Validity.DENIED;
            }
        }
        if (_baseTypeMatchers != null) {
            for (TypeMatcher m : _baseTypeMatchers) {
                if (m.match(rawBase)) {
                    return Validity.ALLOWED;
                }
            }
        }
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType,
            String subClassName)
        throws JsonMappingException
    {
        if (_subTypeNameMatchers != null)  {
            for (NameMatcher m : _subTypeNameMatchers) {
                if (m.match(subClassName)) {
                    return Validity.ALLOWED;
                }
            }
        }
        // could not yet decide, so:
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType, JavaType subType)
            throws JsonMappingException {
        if (_subClassMatchers != null)  {
            final Class<?> subClass = subType.getRawClass();
            for (TypeMatcher m : _subClassMatchers) {
                if (m.match(subClass)) {
                    return Validity.ALLOWED;
                }
            }
        }
        // could not decide, callers gets to decide; usually will deny
        return Validity.INDETERMINATE;
    }
}

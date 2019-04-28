package com.fasterxml.jackson.databind.jsontype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

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
    extends PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Helper classes: matchers
    /**********************************************************
     */

    /**
     * Matcher used before resolving subtype into class
     */
    protected abstract static class ByNameMatcher {
        public abstract Validity match(Class<?> baseType, String subClassName);
    }

    /**
     * Matcher used after resolving subtype into class
     */
    protected abstract static class SubTypeMatcher {
        public abstract Validity match(Class<?> subClass);
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
        protected Set<Class<?>> _invalidBaseTypes;

        protected List<ByNameMatcher> _byName;
        protected List<SubTypeMatcher> _byType;

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
            return _appendByName(new ByNameMatcher() {
                @Override
                public Validity match(Class<?> baseType, String subClassName) {
                    return baseOfBase.isAssignableFrom(baseType) ? Validity.ALLOWED : null;
                }
            });
        }

        /**
         * Method for appending matcher that will allow all subtypes in cases where
         * nominal base type's class name matches given {@link Pattern}
         *<pre>
         *    builder.allowIfBaseType(Pattern.compile("com\\.mycompany\\.")
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is in package {@code com.mycompany} would allow all legal (assignment-compatible)
         * subtypes.
         */
        public Builder allowIfBaseType(final Pattern patternForBase) {
            return _appendByName(new ByNameMatcher() {
                @Override
                public Validity match(Class<?> baseType, String subClassName) {
                    return patternForBase.matcher(baseType.getName()).matches() ? Validity.ALLOWED : null;
                }
            });
        }

        /**
         * Method for appending matcher that will allow all subtypes in cases where
         * nominal base type's class name starts with specific prefix.
         *<pre>
         *    builder.allowIfBaseType("com.mycompany.")
         *</pre>
         * would indicate that any polymorphic properties where declared base type
         * is in package {@code com.mycompany} would allow all legal (assignment-compatible)
         * subtypes.
         */
        public Builder allowIfBaseType(final String prefixForBase) {
            return _appendByName(new ByNameMatcher() {
                @Override
                public Validity match(Class<?> baseType, String subClassName) {
                    return baseType.getName().startsWith(prefixForBase) ? Validity.ALLOWED : null;
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

        public Builder allowIfSubType(final Class<?> subTypeBase) {
            return _appendBySubType(new SubTypeMatcher() {
                @Override
                public Validity match(Class<?> subClass) {
                    return subTypeBase.isAssignableFrom(subClass) ? Validity.ALLOWED : null;
                }
            });
        }

        public Builder allowIfSubType(final Pattern patternForSubType) {
            return _appendByName(new ByNameMatcher() {
                @Override
                public Validity match(Class<?> baseType, String subClassName) {
                    return patternForSubType.matcher(subClassName).matches() ? Validity.ALLOWED : null;
                }
            });
        }

        public Builder allowIfSubType(final String prefixForSubType) {
            return _appendByName(new ByNameMatcher() {
                @Override
                public Validity match(Class<?> baseType, String subClassName) {
                    return subClassName.startsWith(prefixForSubType) ? Validity.ALLOWED : null;
                }
            });
        }

        public BasicPolymorphicTypeValidator build() {
            return new BasicPolymorphicTypeValidator(_invalidBaseTypes,
                    (_byName == null) ? null : _byName.toArray(new ByNameMatcher[0]),
                    (_byType == null) ? null : _byType.toArray(new SubTypeMatcher[0])
            );
        }

        protected Builder _appendByName(ByNameMatcher matcher) {
            if (_byName == null) {
                _byName = new ArrayList<>();
            }
            _byName.add(matcher);
            return this;
        }

        protected Builder _appendBySubType(SubTypeMatcher matcher) {
            if (_byType == null) {
                _byType = new ArrayList<>();
            }
            _byType.add(matcher);
            return this;
        }
    }
    
    
    /*
    /**********************************************************
    /* Actual implementation
    /**********************************************************
     */

    protected final Set<Class<?>> _invalidBaseTypes;
    protected final ByNameMatcher[] _byNameMatchers;
    protected final SubTypeMatcher[] _subTypeMatchers;

    protected BasicPolymorphicTypeValidator(Set<Class<?>> invalidBaseTypes,
            ByNameMatcher[] byNameMatchers,
            SubTypeMatcher[] subTypeMatchers) {
        _invalidBaseTypes = invalidBaseTypes;
        _byNameMatchers = byNameMatchers;
        _subTypeMatchers = subTypeMatchers;
    }

    public static Builder builder() {
        return new Builder();
    }

    // !!! TODO
    @Override
    public Validity validateBaseType(MapperConfig<?> ctxt, JavaType baseType) {
        return Validity.INDETERMINATE;
    }
    
    @Override
    public Validity validateSubClassName(MapperConfig<?> ctxt, JavaType baseType, String subClassName)
            throws JsonMappingException
    {
        if (_byNameMatchers != null)  {
            final Class<?> baseClass = baseType.getRawClass();
            for (ByNameMatcher m : _byNameMatchers) {
                Validity vld = m.match(baseClass, subClassName);
                if (vld != null) {
                    return vld;
                }
            }
        }
        // could not yet decide, so:
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(MapperConfig<?> ctxt, JavaType baseType, JavaType subType)
            throws JsonMappingException {
        if (_subTypeMatchers != null)  {
            final Class<?> subClass = subType.getRawClass();
            for (SubTypeMatcher m : _subTypeMatchers) {
                Validity vld = m.match(subClass);
                if (vld != null) {
                    return vld;
                }
            }
        }
        // could not decide, callers gets to decide...
        return Validity.INDETERMINATE;
    }
}

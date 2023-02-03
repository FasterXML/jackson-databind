package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Configurable handler used to select aspects of selecting
 * constructor to use as "Creator" for POJOs.
 * Defines the API for handlers, a pre-defined set of standard instances
 * and methods for constructing alternative configurations.
 *
 * @since 2.12
 */
public final class ConstructorDetector
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Definition of alternate handling modes of single-argument constructors
     * that are annotated with {@link com.fasterxml.jackson.annotation.JsonCreator}
     * but without "mode" definition (or explicit name for the argument):
     * this is the case where two interpretations
     * are possible -- "properties" (in which case the argument is named parameter
     * of a JSON Object) and "delegating (in which case the argument maps to the
     * whole JSON value).
     *<p>
     * Default choice is {@code HEURISTIC} (which is Jackson pre-2.12 always uses)
     *<p>
     * NOTE: does NOT have any effect if explicit {@code @JsonCreator}} annotation
     * is required.
     *
     * @since 2.12
     */
    public enum SingleArgConstructor {
        /**
         * Assume "delegating" mode if not explicitly annotated otherwise
         */
        DELEGATING,

        /**
         * Assume "properties" mode if not explicitly annotated otherwise
         */
        PROPERTIES,

        /**
         * Use heuristics to see if "properties" mode is to be used (POJO has a
         * property with the same name as the implicit name [if available] of
         * the constructor argument).
         * Note: this is the default choice for Jackson versions before 2.12.
         */
        HEURISTIC,

        /**
         * Refuse to decide implicit mode and instead throw a
         * {@link com.fasterxml.jackson.databind.exc.InvalidDefinitionException}
         * in ambiguous case.
         */
        REQUIRE_MODE;
    }

    /*
    /**********************************************************************
    /* Global default instances to use
    /**********************************************************************
     */

    /**
     * Instance used by default, which:
     *<ul>
     * <li>Uses {@link SingleArgConstructor#HEURISTIC} for single-argument constructor case
     *  </li>
     * <li>Does not require explicit {@code @JsonCreator} annotations (so allows
     * auto-detection of Visible constructors} (except for JDK types)
     *  </li>
     * <li>Does not allow auto-detection of Visible constructors for so-called JDK
     *   types; that is, classes in packages {@code java.*} and {@code javax.*}
     *  </li>
     *</ul>
     */
    public final static ConstructorDetector DEFAULT
        = new ConstructorDetector(SingleArgConstructor.HEURISTIC);

    /**
     * Instance similar to {@link #DEFAULT} except that for single-argument case
     * uses setting of {@link SingleArgConstructor#PROPERTIES}.
     */
    public final static ConstructorDetector USE_PROPERTIES_BASED
        = new ConstructorDetector(SingleArgConstructor.PROPERTIES);

    /**
     * Instance similar to {@link #DEFAULT} except that for single-argument case
     * uses setting of {@link SingleArgConstructor#DELEGATING}.
     */
    public final static ConstructorDetector USE_DELEGATING
        = new ConstructorDetector(SingleArgConstructor.DELEGATING);

    /**
     * Instance similar to {@link #DEFAULT} except that for single-argument case
     * uses setting of {@link SingleArgConstructor#REQUIRE_MODE}.
     */
    public final static ConstructorDetector EXPLICIT_ONLY
        = new ConstructorDetector(SingleArgConstructor.REQUIRE_MODE);

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    protected final SingleArgConstructor _singleArgMode;

    /**
     * Whether explicit {@link com.fasterxml.jackson.annotation.JsonCreator}
     * is always required for detecting constructors (even if visible) other
     * than the default (no argument) constructor.
     */
    protected final boolean _requireCtorAnnotation;

    /**
     * Whether auto-detection of constructors of "JDK types" (those in
     * packages {@code java.} and {@code javax.}) is allowed or not
     */
    protected final boolean _allowJDKTypeCtors;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ConstructorDetector(SingleArgConstructor singleArgMode,
            boolean requireCtorAnnotation,
            boolean allowJDKTypeCtors)
    {
        _singleArgMode = singleArgMode;
        _requireCtorAnnotation = requireCtorAnnotation;
        _allowJDKTypeCtors = allowJDKTypeCtors;
    }

    /**
     * Constructors used for default configurations which only varies
     * by {@code _singleArgMode}
     */
    protected ConstructorDetector(SingleArgConstructor singleArgMode) {
        this(singleArgMode, false, false);
    }

    public ConstructorDetector withSingleArgMode(SingleArgConstructor singleArgMode) {
        return new ConstructorDetector(singleArgMode,
                _requireCtorAnnotation, _allowJDKTypeCtors);
    }

    public ConstructorDetector withRequireAnnotation(boolean state) {
        return new ConstructorDetector(_singleArgMode,
                state, _allowJDKTypeCtors);
    }

    public ConstructorDetector withAllowJDKTypeConstructors(boolean state) {
        return new ConstructorDetector(_singleArgMode,
                _requireCtorAnnotation, state);
    }

    /*
    /**********************************************************************
    /* API
    /**********************************************************************
     */

    public SingleArgConstructor singleArgMode() {
        return _singleArgMode;
    }

    public boolean requireCtorAnnotation() {
        return _requireCtorAnnotation;
    }

    public boolean allowJDKTypeConstructors() {
        return _allowJDKTypeCtors;
    }

    public boolean singleArgCreatorDefaultsToDelegating() {
        return _singleArgMode == SingleArgConstructor.DELEGATING;
    }

    public boolean singleArgCreatorDefaultsToProperties() {
        return _singleArgMode == SingleArgConstructor.PROPERTIES;
    }

    /**
     * Accessor that combines checks for whether implicit creators are allowed
     * and, if so, whether JDK type constructors are allowed (if type is JDK type)
     * to determine whether implicit constructor
     * detection should be enabled for given type or not.
     *
     * @param rawType Value type to consider
     *
     * @return True if implicit constructor detection should be enabled; false if not
     */
    public boolean shouldIntrospectorImplicitConstructors(Class<?> rawType) {
        // May not allow implicit creator introspection at all:
        if (_requireCtorAnnotation) {
            return false;
        }
        // But if it is allowed, may further limit use for JDK types
        if (!_allowJDKTypeCtors) {
            if (ClassUtil.isJDKClass(rawType)) {
                // 18-Sep-2020, tatu: Looks like must make an exception for Exception
                //    types (ha!) -- at this point, single-String-arg constructor
                //    is to be auto-detected
                if (!Throwable.class.isAssignableFrom(rawType)) {
                    return false;
                }
            }
        }
        return true;
    }
}

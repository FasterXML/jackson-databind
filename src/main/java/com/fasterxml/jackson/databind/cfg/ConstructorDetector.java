package com.fasterxml.jackson.databind.cfg;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.introspect.AnnotatedConstructor;

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
     * NOTE: does NOT have any effect if explicit {@link @JsonCreator}} annotation
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

//    @FunctionalInterface
    /**
     * Simple interface for optionally defined handler that can select
     * specific Constructor to use if more than one implicitly detected ones
     * (and no explicitly annotated one) found.
     */
    public interface ConstructorSelector {
        public AnnotatedConstructor select(DatabindContext ctxt,
                List<AnnotatedConstructor> ctors)
            throws IOException;
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
     * <li>Does not specify {@link ConstructorSelector} to solve ambiguous
     *  (multiple implicitly discovered argument-taking Constructors}
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

    protected final ConstructorSelector _selector;

    /**
     * Whether explicit {@link com.fasterxml.jackson.annotation.JsonCreator}
     * is always required for detecting constructors (even if visible) other
     * than the default (no argument) constructor.
     */
    protected final boolean _requireCtorAnnotation;

    /**
     * Whether auto-detection of constructors of "JDK types" (those in
     * packages {@code java.} and {@code javax.}) is allowed or not (
     */
    protected final boolean _allowJDKTypeCtors;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected ConstructorDetector(SingleArgConstructor singleArgMode,
            ConstructorSelector selector,
            boolean requireCtorAnnotation,
            boolean allowJDKTypeCtors)
    {
        _singleArgMode = singleArgMode;
        _selector = selector;
        _requireCtorAnnotation = requireCtorAnnotation;
        _allowJDKTypeCtors = allowJDKTypeCtors;
    }

    /**
     * Constructors used for default configurations which only varies
     * by {@code _singleArgMode}
     */
    protected ConstructorDetector(SingleArgConstructor singleArgMode) {
        this(singleArgMode, null, false, false);
    }

    protected ConstructorDetector withSingleArgMode(SingleArgConstructor singleArgMode) {
        return new ConstructorDetector(singleArgMode, _selector,
                _requireCtorAnnotation, _allowJDKTypeCtors);
    }

    protected ConstructorDetector withSelector(ConstructorSelector selector) {
        return new ConstructorDetector(_singleArgMode, selector,
                _requireCtorAnnotation, _allowJDKTypeCtors);
    }

    protected ConstructorDetector withRequireAnnotation(boolean state) {
        return new ConstructorDetector(_singleArgMode, _selector,
                state, _allowJDKTypeCtors);
    }

    protected ConstructorDetector withAllowJDKTypes(boolean state) {
        return new ConstructorDetector(_singleArgMode, _selector,
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

    public ConstructorSelector constructorSelector() {
        return _selector;
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
}

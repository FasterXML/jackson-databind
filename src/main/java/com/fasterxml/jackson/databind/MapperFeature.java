package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.ConfigFeature;

/**
 * Enumeration that defines simple on/off features to set
 * for {@link ObjectMapper}, and accessible (but not changeable)
 * via {@link ObjectReader} and {@link ObjectWriter} (as well as
 * through various convenience methods through context objects).
 *<p>
 * Note that in addition to being only mutable via {@link ObjectMapper},
 * changes only take effect when done <b>before any serialization or
 * deserialization</b> calls -- that is, caller must follow
 * "configure-then-use" pattern.
 */
public enum MapperFeature implements ConfigFeature
{
    /*
    /******************************************************
    /* Introspection features
    /******************************************************
     */
    
    /**
     * Feature that determines whether annotation introspection
     * is used for configuration; if enabled, configured
     * {@link AnnotationIntrospector} will be used: if disabled,
     * no annotations are considered.
     *<p>
     * Feature is enabled by default.
     */
    USE_ANNOTATIONS(true),

    /**
     * Feature that determines whether "creator" methods are
     * automatically detected by consider public constructors,
     * and static single argument methods with name "valueOf".
     * If disabled, only methods explicitly annotated are considered
     * creator methods (except for the no-arg default constructor which
     * is always considered a factory method).
     *<p>
     * Note that this feature has lower precedence than per-class
     * annotations, and is only used if there isn't more granular
     * configuration available.
     *<P>
     * Feature is enabled by default.
     */
    AUTO_DETECT_CREATORS(true),
    
    /**
     * Feature that determines whether non-static fields are recognized as
     * properties.
     * If yes, then all public member fields
     * are considered as properties. If disabled, only fields explicitly
     * annotated are considered property fields.
     *<p>
     * Note that this feature has lower precedence than per-class
     * annotations, and is only used if there isn't more granular
     * configuration available.
     *<p>
     * Feature is enabled by default.
     */
     AUTO_DETECT_FIELDS(true),
    
    /**
     * Feature that determines whether regular "getter" methods are
     * automatically detected based on standard Bean naming convention
     * or not. If yes, then all public zero-argument methods that
     * start with prefix "get" 
     * are considered as getters.
     * If disabled, only methods explicitly  annotated are considered getters.
     *<p>
     * Note that since version 1.3, this does <b>NOT</b> include
     * "is getters" (see {@link #AUTO_DETECT_IS_GETTERS} for details)
     *<p>
     * Note that this feature has lower precedence than per-class
     * annotations, and is only used if there isn't more granular
     * configuration available.
     *<p>
     * Feature is enabled by default.
     */
    AUTO_DETECT_GETTERS(true),

    /**
     * Feature that determines whether "is getter" methods are
     * automatically detected based on standard Bean naming convention
     * or not. If yes, then all public zero-argument methods that
     * start with prefix "is", and whose return type is boolean
     * are considered as "is getters".
     * If disabled, only methods explicitly annotated are considered getters.
     *<p>
     * Note that this feature has lower precedence than per-class
     * annotations, and is only used if there isn't more granular
     * configuration available.
     *<p>
     * Feature is enabled by default.
     */
    AUTO_DETECT_IS_GETTERS(true),

     /**
      * Feature that determines whether "setter" methods are
      * automatically detected based on standard Bean naming convention
      * or not. If yes, then all public one-argument methods that
      * start with prefix "set"
      * are considered setters. If disabled, only methods explicitly
      * annotated are considered setters.
      *<p>
      * Note that this feature has lower precedence than per-class
      * annotations, and is only used if there isn't more granular
      * configuration available.
      *<P>
      * Feature is enabled by default.
      */
     AUTO_DETECT_SETTERS(true),
     
    /**
     * Feature that determines whether getters (getter methods)
     * can be auto-detected if there is no matching mutator (setter,
     * constructor parameter or field) or not: if set to true,
     * only getters that match a mutator are auto-discovered; if
     * false, all auto-detectable getters can be discovered.
     *<p>
     * Feature is disabled by default.
     */
    REQUIRE_SETTERS_FOR_GETTERS(false),

    /**
     * Feature that determines whether otherwise regular "getter"
     * methods (but only ones that handle Collections and Maps,
     * not getters of other type)
     * can be used for purpose of getting a reference to a Collection
     * and Map to modify the property, without requiring a setter
     * method.
     * This is similar to how JAXB framework sets Collections and
     * Maps: no setter is involved, just setter.
     *<p>
     * Note that such getters-as-setters methods have lower
     * precedence than setters, so they are only used if no
     * setter is found for the Map/Collection property.
     *<p>
     * Feature is enabled by default.
     */
    USE_GETTERS_AS_SETTERS(true),

    /**
     * Feature that determines whether method and field access
     * modifier settings can be overridden when accessing
     * properties. If enabled, method
     * {@link java.lang.reflect.AccessibleObject#setAccessible}
     * may be called to enable access to otherwise unaccessible
     * objects.
     *<p>
     * Note that this setting usually has significant performance implications,
     * since access override helps remove costly access checks on each
     * and every Reflection access. If you are considering disabling
     * this feature, be sure to verify performance consequences if usage
     * is performance sensitive.
     * Especially on standard JavaSE platforms difference is significant.
     *<p>
     * Conversely, on some platforms, it may be necessary to disable this feature
     * as platform does not allow such calls. For example, when developing
     * Applets (or other Java code that runs on tightly restricted sandbox),
     * it may be necessary to disable the feature regardless of performance effects.
     *<p>
     * Feature is enabled by default.
     */
    CAN_OVERRIDE_ACCESS_MODIFIERS(true),

    /**
     * Feature that determines whether member mutators (fields and
     * setters) may be "pulled in" even if they are not visible,
     * as long as there is a visible accessor (getter or field) with same name.
     * For example: field "value" may be inferred as mutator,
     * if there is visible or explicitly marked getter "getValue()".
     * If enabled, inferring is enabled; otherwise (disabled) only visible and
     * explicitly annotated accessors are ever used.
     *<p>
     * Note that 'getters' are never inferred and need to be either visible (including
     * bean-style naming) or explicitly annotated.
     *<p>
     * Feature is enabled by default.
     * 
     * @since 2.2
     */
    INFER_PROPERTY_MUTATORS(true),

    /**
     * Feature that determines whether member fields declared as 'final' may
     * be auto-detected to be used mutators (used to change value of the logical
     * property) or not. If enabled, 'final' access modifier has no effect, and
     * such fields may be detected according to usual visibility and inference
     * rules; if disabled, such fields are NOT used as mutators except if
     * explicitly annotated for such use.
     *<p>
     * Feature is enabled by default, for backwards compatibility reasons.
     * 
     * @since 2.2
     */
    ALLOW_FINAL_FIELDS_AS_MUTATORS(true),

    /**
     * Feature that determines for <code>transient</code> modifier for fields
     * is handled: if disabled, it is only taken to mean exclusion of
     *<p>
     * Feature is disabled by default, meaning that existence of `transient`
     * for a field does not necessarily lead to ignoral of getters or setters.
     *
     * @since 2.6
     */
    PROPAGATE_TRANSIENT_MARKER(false),

    /*
    /******************************************************
    /* Type-handling features
    /******************************************************
     */

    /**
     * Feature that determines whether the type detection for
     * serialization should be using actual dynamic runtime type,
     * or declared static type.
     * Note that deserialization always uses declared static types
     * since no runtime types are available (as we are creating
     * instances after using type information).
     *<p>
     * This global default value can be overridden at class, method
     * or field level by using {@link JsonSerialize#typing} annotation
     * property.
     *<p>
     * Feature is disabled by default which means that dynamic runtime types
     * are used (instead of declared static types) for serialization.
     */
    USE_STATIC_TYPING(false),

    /*
    /******************************************************
    /* View-related features
    /******************************************************
     */
    
    /**
     * Feature that determines whether properties that have no view
     * annotations are included in JSON serialization views (see
     * {@link com.fasterxml.jackson.annotation.JsonView} for more
     * details on JSON Views).
     * If enabled, non-annotated properties will be included;
     * when disabled, they will be excluded. So this feature
     * changes between "opt-in" (feature disabled) and
     * "opt-out" (feature enabled) modes.
     *<p>
     * Default value is enabled, meaning that non-annotated
     * properties are included in all views if there is no
     * {@link com.fasterxml.jackson.annotation.JsonView} annotation.
     *<p>
     * Feature is enabled by default.
     */
    DEFAULT_VIEW_INCLUSION(true),
    
    /*
    /******************************************************
    /* Generic output features
    /******************************************************
     */

    /**
     * Feature that defines default property serialization order used
     * for POJO fields (note: does <b>not</b> apply to {@link java.util.Map}
     * serialization!):
     * if enabled, default ordering is alphabetic (similar to
     * how {@link com.fasterxml.jackson.annotation.JsonPropertyOrder#alphabetic()}
     * works); if disabled, order is unspecified (based on what JDK gives
     * us, which may be declaration order, but is not guaranteed).
     *<p>
     * Note that this is just the default behavior, and can be overridden by
     * explicit overrides in classes (for example with
     * {@link com.fasterxml.jackson.annotation.JsonPropertyOrder} annotation)
     *<p>
     * Feature is disabled by default.
     */
    SORT_PROPERTIES_ALPHABETICALLY(false),

    /*
    /******************************************************
    /* Name-related features
    /******************************************************
     */
    /**
     * Feature that will allow for more forgiving deserialization of incoming JSON.
     * If enabled, the bean properties will be matched using their lower-case equivalents,
     * meaning that any case-combination (incoming and matching names are canonicalized
     * by lower-casing) should work.
     *<p>
     * Note that there is additional performance overhead since incoming property
     * names need to be lower-cased before comparison, for cases where there are upper-case
     * letters. Overhead for names that are already lower-case should be negligible however.
     *<p>
     * Feature is disabled by default.
     * 
     * @since 2.5
     */
    ACCEPT_CASE_INSENSITIVE_PROPERTIES(false),
    
    /**
     * Feature that can be enabled to make property names be
     * overridden by wrapper name (usually detected with annotations
     * as defined by {@link AnnotationIntrospector#findWrapperName}.
     * If enabled, all properties that have associated non-empty Wrapper
     * name will use that wrapper name instead of property name.
     * If disabled, wrapper name is only used for wrapping (if anything).
     *<p>
     * Feature is disabled by default.
     * 
     * @since 2.1
     */
    USE_WRAPPER_NAME_AS_PROPERTY_NAME(false),

    /**
     * Feature that may be enabled to enforce strict compatibility with
     * Bean name introspection, instead of slightly different mechanism
     * Jackson defaults to.
     * Specific difference is that Jackson always lower cases leading upper-case
     * letters, so "getURL()" becomes "url" property; whereas standard Bean
     * naming <b>only</b> lower-cases the first letter if it is NOT followed by
     * another upper-case letter (so "getURL()" would result in "URL" property).
     *<p>
     * Feature is disabled by default for backwards compatibility purposes: earlier
     * Jackson versions used Jackson's own mechanism.
     *
     * @since 2.5
     */
    USE_STD_BEAN_NAMING(false),

    /*
    /******************************************************
    /* Other features
    /******************************************************
     */

    /**
     * Feature that determines whether multiple registrations of same module
     * should be ignored or not; if enabled, only the first registration call
     * results in module being called, and possible duplicate calls are silently
     * ignored; if disabled, no checking is done and all registration calls are
     * dispatched to module.
     *<p>
     * Definition of "same module" is based on using {@link Module#getTypeId()};
     * modules with same non-null <code>type id</code> are considered same for
     * purposes of duplicate registration. This also avoids having to keep track
     * of actual module instances; only ids will be kept track of (and only if
     * this feature is enabled).
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.5
     */
    IGNORE_DUPLICATE_MODULE_REGISTRATIONS(true)
    
    ;

    private final boolean _defaultState;
    private final int _mask;
    
    private MapperFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }
    
    @Override
    public boolean enabledByDefault() { return _defaultState; }

    @Override
    public int getMask() { return _mask; }

    /**
     * @since 2.5
     */
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
}

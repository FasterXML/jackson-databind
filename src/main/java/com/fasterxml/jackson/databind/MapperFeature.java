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
    /* General introspection features
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
     * Feature is disabled by default since 3.0 (with 2.x was enabled)
     */
    USE_GETTERS_AS_SETTERS(false),

    /**
     * Feature that determines how <code>transient</code> modifier for fields
     * is handled: if disabled, it is only taken to mean exclusion of the field
     * as accessor; if true, it is taken to imply removal of the whole property.
     *<p>
     * Feature is disabled by default, meaning that existence of `transient`
     * for a field does not necessarily lead to ignoral of getters or setters
     * but just ignoring the use of field for access.
     */
    PROPAGATE_TRANSIENT_MARKER(false),

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
     * Feature that determines whether member fields declared as 'final' may
     * be auto-detected to be used mutators (used to change value of the logical
     * property) or not. If enabled, 'final' access modifier has no effect, and
     * such fields may be detected according to usual visibility and inference
     * rules; if disabled, such fields are NOT used as mutators except if
     * explicitly annotated for such use.
     *<p>
     * Feature is enabled by default, for backwards compatibility reasons.
     */
    ALLOW_FINAL_FIELDS_AS_MUTATORS(true),

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
     */
    INFER_PROPERTY_MUTATORS(true),

    /**
     * Feature that determines handling of {@code java.beans.ConstructorProperties}
     * annotation: when enabled, it is considered as alias of
     * {@link com.fasterxml.jackson.annotation.JsonCreator}, to mean that constructor
     * should be considered a property-based Creator; when disabled, only constructor
     * parameter name information is used, but constructor is NOT considered an explicit
     * Creator (although may be discovered as one using other annotations or heuristics).
     *<p>
     * Feature is mostly used to help inter-operability with frameworks like Lombok
     * that may automatically generate {@code ConstructorProperties} annotation
     * but without necessarily meaning that constructor should be used as Creator
     * for deserialization.
     *<p>
     * Feature is enabled by default.
     */
    INFER_CREATOR_FROM_CONSTRUCTOR_PROPERTIES(true),

    /*
    /******************************************************
    /* Access modifier handling
    /******************************************************
     */

    /**
     * Feature that determines whether method and field access
     * modifier settings can be overridden when accessing
     * properties. If enabled, method
     * {@link java.lang.reflect.AccessibleObject#setAccessible}
     * may be called to enable access to otherwise unaccessible objects.
     *<p>
     * Note that this setting may have significant performance implications,
     * since access override helps remove costly access checks on each
     * and every Reflection access. If you are considering disabling
     * this feature, be sure to verify performance consequences if usage
     * is performance sensitive.
     * Also note that performance effects vary between Java platforms
     * (JavaSE vs Android, for example), as well as JDK versions: older
     * versions seemed to have more significant performance difference.
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
     * Feature that determines that forces call to
     * {@link java.lang.reflect.AccessibleObject#setAccessible} even for
     * <code>public</code> accessors -- that is, even if no such call is
     * needed from functionality perspective -- if call is allowed
     * (that is, {@link #CAN_OVERRIDE_ACCESS_MODIFIERS} is set to true).
     * The main reason to enable this feature is possible performance
     * improvement as JDK does not have to perform access checks; these
     * checks are otherwise made for all accessors, including public ones,
     * and may result in slower Reflection calls. Exact impact (if any)
     * depends on Java platform (Java SE, Android) as well as JDK version.
     *<p>
     * Feature is enabled by default, for legacy reasons (it was the behavior
     * until 2.6)
     */
    OVERRIDE_PUBLIC_ACCESS_MODIFIERS(true),

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

    /**
     * Feature that enables inferring builder type bindings from the value type
     * being deserialized. This requires that the generic type declaration on
     * the value type match that on the builder exactly.
     *<p>
     * Feature is disabled by default which means that deserialization does
     * not support deserializing types via builders with type parameters.
     *<p>
     * See: https://github.com/FasterXML/jackson-databind/issues/921
     */
    INFER_BUILDER_TYPE_BINDINGS(false),

    /**
     * Feature that specifies whether the declared base type of a polymorphic value
     * is to be used as the "default" implementation, if no explicit default class
     * is specified via {@code @JsonTypeInfo.defaultImpl} annotation.
     *<p>
     * Note that feature only has effect on deserialization of regular polymorphic properties:
     * it does NOT affect non-polymorphic cases, and is unlikely to work with Default Typing.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     *
     * @since 2.9.6
     */
    USE_BASE_TYPE_AS_DEFAULT_IMPL(false),

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
     */
    ACCEPT_CASE_INSENSITIVE_PROPERTIES(false),


    /**
     * Feature that determines if Enum deserialization should be case sensitive or not.
     * If enabled, Enum deserialization will ignore case, that is, case of incoming String
     * value and enum id (dependant on other settings, either `name()`, `toString()`, or
     * explicit override) do not need to match.
     * <p>
     * Feature is disabled by default.
     */
    ACCEPT_CASE_INSENSITIVE_ENUMS(false),

    /**
     * Feature that can be enabled to make property names be
     * overridden by wrapper name (usually detected with annotations
     * as defined by {@link AnnotationIntrospector#findWrapperName}.
     * If enabled, all properties that have associated non-empty Wrapper
     * name will use that wrapper name instead of property name.
     * If disabled, wrapper name is only used for wrapping (if anything).
     *<p>
     * Feature is disabled by default.
     */
    USE_WRAPPER_NAME_AS_PROPERTY_NAME(false),

    /**
     * Feature that when enabled will allow explicitly named properties (i.e., fields or methods
     * annotated with {@link com.fasterxml.jackson.annotation.JsonProperty}("explicitName")) to
     * be re-named by a {@link PropertyNamingStrategy}, if one is configured.
     * <p>
     * Feature is disabled by default.
     */
    ALLOW_EXPLICIT_PROPERTY_RENAMING(false),

    /*
    /******************************************************
    /* Other features
    /******************************************************
     */

    /**
     * Setting that determines what happens if an attempt is made to explicitly
     * "merge" value of a property, where value does not support merging; either
     * merging is skipped and new value is created (<code>true</code>) or
     * an exception is thrown (false).
     *<p>
     * Feature is disabled by default since non-mergeable property types are ignored
     * even if defaults call for merging, and usually explicit per-type or per-property
     * settings for such types should result in an exception.
     */
    IGNORE_MERGE_FOR_UNMERGEABLE(true)

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

    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
}

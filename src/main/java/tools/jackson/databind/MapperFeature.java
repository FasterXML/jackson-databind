package tools.jackson.databind;

import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Enumeration that defines simple on/off features to set
 * for {@link ObjectMapper}, and accessible (but not changeable)
 * via {@link ObjectReader} and {@link ObjectWriter} (as well as
 * through various convenience methods through context objects).
 */
public enum MapperFeature
{
    /*
    /**********************************************************************
    /* General introspection features
    /**********************************************************************
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
     * Maps: no setter is involved, just getter.
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
     * as an accessor; if true, it is taken to imply removal of the whole property.
     *<p>
     * Feature is disabled by default, meaning that existence of `transient`
     * for a field does not necessarily lead to ignoral of getters or setters
     * but just ignoring the use of field for access.
     *<p>
     * NOTE! This should have no effect on <b>explicit</b> ignoral annotation
     * possibly added to {@code transient} fields: those should always have expected
     * semantics (same as if field was not {@code transient}).
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
     * Feature is disabled by default in Jackson 3.x, in 2.x it was enabled by default.
     */
    ALLOW_FINAL_FIELDS_AS_MUTATORS(false),

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

    /**
     * Feature that determines whether nominal property type of {@link Void} is
     * allowed for Getter methods to indicate {@code null} valued pseudo-property
     * or not. If enabled, such properties are recognized (see [databind#2675] for
     * reasons -- mostly things related to frameworks, code generation); if disabled,
     * such property accessors (or at least getters) are ignored.
     *<p>
     * Feature is enabled by default.
     */
    ALLOW_VOID_VALUED_PROPERTIES(true),

    /*
    /**********************************************************************
    /* Access modifier handling
    /**********************************************************************
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
    /**********************************************************************
    /* Type-handling features
    /**********************************************************************
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
     * Feature that specifies whether the declared base type of a polymorphic value
     * is to be used as the "default" implementation, if no explicit default class
     * is specified via {@code @JsonTypeInfo.defaultImpl} annotation.
     *<p>
     * Note that feature only has effect on deserialization of regular polymorphic properties:
     * it does NOT affect non-polymorphic cases, and is unlikely to work with Default Typing.
     *<p>
     * Feature is disabled by default for backwards compatibility.
     */
    USE_BASE_TYPE_AS_DEFAULT_IMPL(false),

    /**
     * Feature that enables inferring builder type bindings from the value type
     * being deserialized. This requires that the generic type declaration on
     * the value type match that on the builder exactly: mismatched type declarations
     * are not necessarily detected by databind.
     *<p>
     * Feature is enabled by default which means that deserialization does
     * support deserializing types via builders with type parameters (generic types).
     *<p>
     * See: <a href="https://github.com/FasterXML/jackson-databind/issues/921">databind#921</a>
     */
    INFER_BUILDER_TYPE_BINDINGS(true),

    /**
     * Feature that determines what happens when deserializing to a registered sub-type
     * (polymorphic deserialization), but no type information has been provided.
     * If enabled, then an {@code InvalidTypeIdException} will be thrown;
     * if disabled then the deserialization may proceed without the type information
     * if sub-type is legit target (non-abstract).
     *<p>
     * Feature is enabled by default.
     */
    REQUIRE_TYPE_ID_FOR_SUBTYPES(true),

    /*
    /**********************************************************************
    /* View-related features
    /**********************************************************************
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
    /**********************************************************************
    /* Generic output features
    /**********************************************************************
     */

    /**
     * Feature that defines default property serialization order used
     * for POJO properties.
     * If enabled, default ordering is alphabetic (similar to
     * how {@link com.fasterxml.jackson.annotation.JsonPropertyOrder#alphabetic()}
     * works); if disabled, order is unspecified (based on what JDK gives
     * us, which may be declaration order, but is not guaranteed).
     *<p>
     * Note that this is just the default behavior and can be overridden by
     * explicit overrides in classes (for example with
     * {@link com.fasterxml.jackson.annotation.JsonPropertyOrder} annotation)
     *<p>
     * Note: does <b>not</b> apply to {@link java.util.Map} serialization (since
     * entries are not considered Bean/POJO properties.
     *<p>
     * Feature is disabled by default.
     */
    SORT_PROPERTIES_ALPHABETICALLY(false),

    /**
     * Feature that defines whether Creator properties (ones passed through
     * constructor or static factory method) should be sorted before other properties
     * for which no explicit order is specified, in case where alphabetic
     * ordering is to be used for such properties.
     * Note that in either case explicit order (whether by name or by index)
     * will have precedence over this setting.
     *<p>
     * Note: does <b>not</b> apply to {@link java.util.Map} serialization (since
     * entries are not considered Bean/POJO properties.
     * <p>
     * WARNING: Disabling it may have a negative impact on deserialization performance.
     * When disabled, all properties before the last creator property in the input need to be buffered,
     * since all creator properties are required to create the instance.
     * Enabling this feature ensures that there is as little buffering as possible. 
     *<p>
     * Feature is enabled by default.
     */
    SORT_CREATOR_PROPERTIES_FIRST(true),

    /*
    /**********************************************************************
    /* Name-related features
    /**********************************************************************
     */

    /**
     * Feature that will allow for more forgiving deserialization of incoming JSON.
     * If enabled, the bean properties will be matched using their lower-case equivalents,
     * meaning that any case-combination (incoming and matching names are canonicalized
     * by lower-casing) should work.
     *<p>
     * Note that there is additional performance overhead since incoming property
     * names need to be lower-cased before comparison, for cases where there are upper-case
     * letters. Overhead for names that are already lower-case should be negligible.
     *<p>
     * Feature is disabled by default.
     */
    ACCEPT_CASE_INSENSITIVE_PROPERTIES(false),

    /**
     * Feature that determines if Enum deserialization should be case sensitive or not.
     * If enabled, Enum deserialization will ignore case, that is, case of incoming String
     * value and enum id (depending on other settings, either `name()`, `toString()`, or
     * explicit override) do not need to match.
     *<p>
     * This allows both Enum-as-value deserialization and Enum-as-Map-key, unlike some
     * other settings that are separate for value/key handling.
     *<p>
     * Feature is disabled by default.
     */
    ACCEPT_CASE_INSENSITIVE_ENUMS(false),

    /**
     * Feature that permits parsing some enumerated text-based value types but ignoring the case
     * of the values on deserialization: for example, date/time type deserializers.
     * Support for this feature depends on deserializer implementations using it.
     *<p>
     * Note, however, that regular {@code Enum} types follow {@link #ACCEPT_CASE_INSENSITIVE_ENUMS}
     * setting instead.
     *<p>
     * Feature is disabled by default.
     */
    ACCEPT_CASE_INSENSITIVE_VALUES(false),

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

    /**
     * Feature that when enabled will allow getters with is-Prefix also for
     * non-boolean return types; if disabled only methods that return
     * {@code boolean} or {@code Boolean} qualify as "is getters".
     * <p>
     * Feature is disabled by default for backwards compatibility.
     *
     * @since 2.14
     */
    ALLOW_IS_GETTERS_FOR_NON_BOOLEAN(false),

    /*
    /**********************************************************************
    /* Coercion features
    /**********************************************************************
     */

    /**
     * Feature that determines whether coercions from secondary representations are allowed
     * for simple non-textual scalar types: numbers and booleans. This includes `primitive`
     * types and their wrappers, but excludes `java.lang.String` and date/time types.
     *<p>
     * When feature is disabled, only strictly compatible input may be bound: numbers for
     * numbers, boolean values for booleans. When feature is enabled, conversions from
     * JSON String are allowed, as long as textual value matches (for example, String
     * "true" is allowed as equivalent of JSON boolean token `true`; or String "1.0"
     * for `double`).
     *<p>
     * Note that it is possible that other configurability options can override this
     * in closer scope (like on per-type or per-property basis); this is just the global
     * default.
     *<p>
     * Feature is enabled by default (for backwards compatibility since this was the
     * default behavior)
     */
    ALLOW_COERCION_OF_SCALARS(true),

    /*
    /**********************************************************************
    /* Other features
    /**********************************************************************
     */

    /**
     * Setting that determines what happens if an attempt is made to explicitly
     * "merge" value of a property, where value does not support merging; either
     * merging is skipped and new value is created (<code>true</code>) or
     * an exception is thrown (false).
     *<p>
     * Feature is enabled by default, to allow use of merge defaults even in presence
     * of some unmergeable properties.
     */
    IGNORE_MERGE_FOR_UNMERGEABLE(true),

    /**
     * Feature that determines whether {@link ObjectReader} applies default values
     * defined in class definitions in cases where the input data omits the relevant values.
     *<p>
     * Not all modules will respect this feature. Initially, only {@code jackson-module-scala}
     * will respect this feature but other modules will add support over time.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.13
     */
    APPLY_DEFAULT_VALUES(true)
    ;

    private final boolean _defaultState;
    private final long _mask;

    public static long collectLongDefaults() {
        long flags = 0;
        for (MapperFeature value : MapperFeature.values()) {
            if (value.enabledByDefault()) {
                flags |= value.getLongMask();
            }
        }
        return flags;
    }

    private MapperFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1L << ordinal());
    }

    public boolean enabledByDefault() { return _defaultState; }

    public long getLongMask() {
        return _mask;
    }

    public boolean enabledIn(long flags) {
        return (flags & _mask) != 0;
    }
}

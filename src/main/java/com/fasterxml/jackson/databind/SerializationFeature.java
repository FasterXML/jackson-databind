package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.cfg.ConfigFeature;
import com.fasterxml.jackson.databind.cfg.EnumFeature;

/**
 * Enumeration that defines simple on/off features that affect
 * the way Java objects are serialized.
 *<p>
 * Note that features can be set both through
 * {@link ObjectMapper} (as sort of defaults) and through
 * {@link ObjectWriter}.
 * In first case these defaults must follow "config-then-use" patterns
 * (i.e. defined once, not changed afterwards); all per-call
 * changes must be done using {@link ObjectWriter}.
 */
public enum SerializationFeature implements ConfigFeature
{
    /*
    /******************************************************
    /* Generic output features
    /******************************************************
     */

    /**
     * Feature that can be enabled to make root value (usually JSON
     * Object but can be any type) wrapped within a single property
     * JSON object, where key as the "root name", as determined by
     * annotation introspector (esp. for JAXB that uses
     * <code>@XmlRootElement.name</code>) or fallback (non-qualified
     * class name).
     * Feature is mostly intended for JAXB compatibility.
     *<p>
     * Feature is disabled by default.
     */
    WRAP_ROOT_VALUE(false),

    /**
     * Feature that allows enabling (or disabling) indentation
     * for the underlying generator, using the default pretty
     * printer configured for {@link ObjectMapper} (and
     * {@link ObjectWriter}s created from mapper).
     *<p>
     * Note that the default pretty printer is only used if
     * no explicit {@link com.fasterxml.jackson.core.PrettyPrinter} has been configured
     * for the generator or {@link ObjectWriter}.
     *<p>
     * Feature is disabled by default.
     */
    INDENT_OUTPUT(false),

    /*
    /******************************************************
    /* Error handling features
    /******************************************************
     */

    /**
     * Feature that determines what happens when no accessors are
     * found for a type (and there are no annotations to indicate
     * it is meant to be serialized). If enabled (default), an
     * exception is thrown to indicate these as non-serializable
     * types; if disabled, they are serialized as empty Objects,
     * i.e. without any properties.
     *<p>
     * Note that empty types that this feature has only effect on
     * those "empty" beans that do not have any recognized annotations
     * (like <code>@JsonSerialize</code>): ones that do have annotations
     * do not result in an exception being thrown.
     *<p>
     * Feature is enabled by default.
     */
    FAIL_ON_EMPTY_BEANS(true),

    /**
     * Feature that determines what happens when a direct self-reference
     * is detected by a POJO (and no Object Id handling is enabled for it):
     * either a {@link JsonMappingException} is
     * thrown (if true), or reference is normally processed (false).
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.4
     */
    FAIL_ON_SELF_REFERENCES(true),

    /**
     * Feature that determines whether Jackson code should catch
     * and wrap {@link Exception}s (but never {@link Error}s!)
     * to add additional information about
     * location (within input) of problem or not. If enabled,
     * most exceptions will be caught and re-thrown (exception
     * specifically being that {@link java.io.IOException}s may be passed
     * as is, since they are declared as throwable); this can be
     * convenient both in that all exceptions will be checked and
     * declared, and so there is more contextual information.
     * However, sometimes calling application may just want "raw"
     * unchecked exceptions passed as is.
     *<p>
     *<p>
     * Feature is enabled by default.
     */
    WRAP_EXCEPTIONS(true),

    /**
     * Feature that determines what happens when an object which
     * normally has type information included by Jackson is used
     * in conjunction with {@link com.fasterxml.jackson.annotation.JsonUnwrapped}.
     * In the default (enabled) state, an error will be thrown when
     * an unwrapped object has type information. When disabled, the
     * object will be unwrapped and the type information discarded.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.4
     */
    FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS(true),

    /**
     * Feature that determines what happens when a direct self-reference is detected
     * by a POJO (and no Object Id handling is enabled for it):
     * if enabled write that reference as null; if disabled, default behavior is
     * used (which will try to serialize usually resulting in exception).
     * But if {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} is enabled. this property is ignored.
     * <p>
     * Feature is disabled by default.
     *
     * @since 2.11
     */
    WRITE_SELF_REFERENCES_AS_NULL(false),

    /*
    /******************************************************
    /* Output life cycle features
    /******************************************************
     */

     /**
      * Feature that determines whether <code>close</code> method of
      * serialized <b>root level</b> objects (ones for which <code>ObjectMapper</code>'s
      * writeValue() (or equivalent) method is called)
      * that implement {@link java.io.Closeable}
      * is called after serialization or not. If enabled, <b>close()</b> will
      * be called after serialization completes (whether succesfully, or
      * due to an error manifested by an exception being thrown). You can
      * think of this as sort of "finally" processing.
      *<p>
      * NOTE: only affects behavior with <b>root</b> objects, and not other
      * objects reachable from the root object. Put another way, only one
      * call will be made for each 'writeValue' call.
     *<p>
     * Feature is disabled by default.
      */
    CLOSE_CLOSEABLE(false),

    /**
     * Feature that determines whether <code>JsonGenerator.flush()</code> is
     * called after <code>writeValue()</code> method <b>that takes JsonGenerator
     * as an argument</b> completes (i.e. does NOT affect methods
     * that use other destinations); same for methods in {@link ObjectWriter}.
     * This usually makes sense; but there are cases where flushing
     * should not be forced: for example when underlying stream is
     * compressing and flush() causes compression state to be flushed
     * (which occurs with some compression codecs).
     *<p>
     * Feature is enabled by default.
     */
    FLUSH_AFTER_WRITE_VALUE(true),

    /*
    /******************************************************
    /* Datatype-specific serialization configuration
    /******************************************************
     */

    /**
     * Feature that determines whether Date (and date/time) values
     * (and Date-based things like {@link java.util.Calendar}s) are to be
     * serialized as numeric time stamps (true; the default),
     * or as something else (usually textual representation).
     * If textual representation is used, the actual format depends on configuration
     * settings including possible per-property use of {@code @JsonFormat} annotation,
     * globally configured {@link java.text.DateFormat}.
     *<p>
     * For "classic" JDK date types ({@link java.util.Date}, {@link java.util.Calendar})
     * the default formatting is provided by {@link com.fasterxml.jackson.databind.util.StdDateFormat},
     * and corresponds to format String of "yyyy-MM-dd'T'HH:mm:ss.SSSX"
     * (see {@link java.text.DateFormat} for details of format Strings).
     * Whether this feature affects handling of other date-related
     * types depend on handlers of those types, although ideally they
     * should use this feature
     *<p>
     * Note: whether {@link java.util.Map} keys are serialized as Strings
     * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS} instead of
     * this feature.
     *<p>
     * Feature is enabled by default, so that date/time are by default
     * serialized as time stamps.
     */
    WRITE_DATES_AS_TIMESTAMPS(true),

    /**
     * Feature that determines whether {@link java.util.Date}s
     * (and sub-types) used as {@link java.util.Map} keys are serialized
     * as time stamps or not (if not, will be serialized as textual values).
     *<p>
     * Default value is 'false', meaning that Date-valued Map keys are serialized
     * as textual (ISO-8601) values.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

    /**
     * Feature that determines whether date/date-time values should be serialized
     * so that they include timezone id, in cases where type itself contains
     * timezone information. Including this information may lead to compatibility
     * issues because ISO-8601 specification does not define formats that include
     * such information.
     *<p>
     * If enabled, Timezone id should be included using format specified
     * with Java 8 <code>DateTimeFormatter#ISO_ZONED_DATE_TIME</code> definition
     * (for example, '2011-12-03T10:15:30+01:00[Europe/Paris]').
     *<p>
     * Note: setting has no relevance if date/time values are serialized as timestamps.
     *<p>
     * Feature is disabled by default, so that zone id is NOT included; rather, timezone
     * offset is used for ISO-8601 compatibility (if any timezone information is
     * included in value).
     *
     * @since 2.6
     */
    WRITE_DATES_WITH_ZONE_ID(false),

    /**
     * Feature that determines whether timezone/offset included in zoned date/time
     * values (note: does NOT {@link java.util.Date} will be overridden if there
     * is an explicitly set context time zone.
     * If disabled, timezone/offset value is used-is; if enabled, context time zone
     * is used instead.
     *<p>
     * Note that this setting only affects "Zoned" date/time values of
     * {@code Java 8 date/time} types -- it will have no effect on old
     * {@link java.util} value handling (of which {@link java.util.Date} has no timezone
     * information and must use contextual timezone, implicit or explicit; and
     * {@link java.util.Calendar} which will always use timezone Calendar value has).
     * Setting is also ignored by Joda date/time values.
     *<p>
     * Featured is enabled by default for backwards-compatibility purposes (in
     * Jackson 2.12 override was always done if there was explicitly defined timezone).
     *
     * @since 2.13
     */
    WRITE_DATES_WITH_CONTEXT_TIME_ZONE(true),

    /**
     * Feature that determines whether time values that represents time periods
     * (durations, periods, ranges) are to be serialized by default using
     * a numeric (true) or textual (false) representations. Note that numeric
     * representation may mean either simple number, or an array of numbers,
     * depending on type.
     *<p>
     * Note: whether {@link java.util.Map} keys are serialized as Strings
     * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS}.
     *<p>
     * Feature is enabled by default, so that period/duration are by default
     * serialized as timestamps.
     *
     * @since 2.5
     */
    WRITE_DURATIONS_AS_TIMESTAMPS(true),

    /**
     * Feature that determines how type <code>char[]</code> is serialized:
     * when enabled, will be serialized as an explict JSON array (with
     * single-character Strings as values); when disabled, defaults to
     * serializing them as Strings (which is more compact).
     *<p>
     * Feature is disabled by default.
     */
    WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS(false),

    /**
     * Feature that determines standard serialization mechanism used for
     * Enum values: if enabled, return value of <code>Enum.toString()</code>
     * is used; if disabled, return value of <code>Enum.name()</code> is used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link DeserializationFeature#READ_ENUMS_USING_TO_STRING}.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_ENUMS_USING_TO_STRING(false),

    /**
     * Feature that determines whether Java Enum values are serialized
     * as numbers (true), or textual values (false). If textual values are
     * used, other settings are also considered.
     * If this feature is enabled,
     *  return value of <code>Enum.ordinal()</code>
     * (an integer) will be used as the serialization.
     *<p>
     * Note that this feature has precedence over {@link #WRITE_ENUMS_USING_TO_STRING},
     * which is only considered if this feature is set to false.
     *<p>
     * Note that since 2.10, this does NOT apply to {@link Enum}s written as
     * keys of {@link java.util.Map} values, which has separate setting,
     * {@link #WRITE_ENUM_KEYS_USING_INDEX}.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_ENUMS_USING_INDEX(false),

    /**
     * Feature that determines whether {link Enum}s
     * used as {@link java.util.Map} keys are serialized
     * as using {@link Enum#ordinal()} or not.
     * Similar to {@link #WRITE_ENUMS_USING_INDEX} used when writing
     * {@link Enum}s as regular values.
     *<p>
     * NOTE: counterpart for this settings is
     * {@link EnumFeature#READ_ENUM_KEYS_USING_INDEX}.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.10
     */
    WRITE_ENUM_KEYS_USING_INDEX(false),

    /**
     * Feature that determines whether Map entries with null values are
     * to be serialized (true) or not (false).
     *<p>
     * NOTE: unlike other {@link SerializationFeature}s, this feature <b>cannot</b> be
     * dynamically changed on per-call basis, because its effect is considered during
     * construction of serializers and property handlers.
     *<p>
     * Feature is enabled by default.
     *
     * @deprecated Since 2.9 there are better mechanism for specifying filtering; specifically
     *   using {@link com.fasterxml.jackson.annotation.JsonInclude} or configuration overrides
     *   (see {@link ObjectMapper#configOverride(Class)}}).
     */
    @Deprecated // since 2.9
    WRITE_NULL_MAP_VALUES(true),

    /**
     * Feature that determines whether Container properties (POJO properties
     * with declared value of Collection or array; i.e. things that produce JSON
     * arrays) that are empty (have no elements)
     * will be serialized as empty JSON arrays (true), or suppressed from output (false).
     *<p>
     * Note that this does not change behavior of {@link java.util.Map}s, or
     * "Collection-like" types.
     *<p>
     * NOTE: unlike other {@link SerializationFeature}s, this feature <b>cannot</b> be
     * dynamically changed on per-call basis, because its effect is considered during
     * construction of serializers and property handlers.
     *<p>
     * Feature is enabled by default.
     *
     * @deprecated Since 2.8 there are better mechanism for specifying filtering; specifically
     *   using {@link com.fasterxml.jackson.annotation.JsonInclude} or configuration overrides.
     */
    @Deprecated // since 2.8
    WRITE_EMPTY_JSON_ARRAYS(true),

    /**
     * Feature added for interoperability, to work with oddities of
     * so-called "BadgerFish" convention.
     * Feature determines handling of single element {@link java.util.Collection}s
     * and arrays: if enabled, {@link java.util.Collection}s and arrays that contain exactly
     * one element will be serialized as if that element itself was serialized.
     *<p>
     * When enabled, a POJO with array that normally looks like this:
     *<pre>
     *  { "arrayProperty" : [ 1 ] }
     *</pre>
     * will instead be serialized as
     *<pre>
     *  { "arrayProperty" : 1 }
     *</pre>
     *<p>
     * Note that this feature is counterpart to {@link DeserializationFeature#ACCEPT_SINGLE_VALUE_AS_ARRAY}
     * (that is, usually both are enabled, or neither is).
     *<p>
     * Feature is disabled by default, so that no special handling is done.
     */
    WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED(false),

    /**
     * Feature that determines whether {@link java.math.BigDecimal} entries are
     * serialized using {@link java.math.BigDecimal#toPlainString()} to prevent
     * values to be written using scientific notation.
     *<p>
     * NOTE: since this feature typically requires use of
     * {@link com.fasterxml.jackson.core.JsonGenerator#writeNumber(String)}
     * it may cause compatibility problems since not all {@link com.fasterxml.jackson.core.JsonGenerator}
     * implementations support such mode of output: usually only text-based formats
     * support it.
     *<p>
     * Feature is disabled by default.
     *
     * @deprecated Since 2.5: use {@link com.fasterxml.jackson.core.JsonGenerator.Feature#WRITE_BIGDECIMAL_AS_PLAIN} instead
     *    (using {@link ObjectWriter#with(com.fasterxml.jackson.core.JsonGenerator.Feature)}).
     */
    @Deprecated // since 2.5
    WRITE_BIGDECIMAL_AS_PLAIN(false),

    /**
     * Feature that controls whether numeric timestamp values are
     * to be written using nanosecond timestamps (enabled) or not (disabled);
     * <b>if and only if</b> datatype supports such resolution.
     * Only newer datatypes (such as Java8 Date/Time) support such resolution --
     * older types (pre-Java8 <b>java.util.Date</b> etc) and Joda do not --
     * and this setting <b>has no effect</b> on such types.
     *<p>
     * If disabled, standard millisecond timestamps are assumed.
     * This is the counterpart to {@link DeserializationFeature#READ_DATE_TIMESTAMPS_AS_NANOSECONDS}.
     *<p>
     * Feature is enabled by default, to support most accurate time values possible.
     *
     * @since 2.2
     */
    WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS(true),

    /**
     * Feature that determines whether {@link java.util.Map} entries are first
     * sorted by key before serialization or not: if enabled, additional sorting
     * step is performed if necessary (not necessary for {@link java.util.SortedMap}s),
     * if disabled, no additional sorting is needed.
     *<p>
     * Feature is disabled by default.
     */
    ORDER_MAP_ENTRIES_BY_KEYS(false),

    /*
    /******************************************************
    /* Other
    /******************************************************
     */

    /**
     * Feature that determines whether {@link ObjectWriter} should
     * try to eagerly fetch necessary {@link JsonSerializer} when
     * possible. This improves performance in cases where similarly
     * configured {@link ObjectWriter} instance is used multiple
     * times; and should not significantly affect single-use cases.
     *<p>
     * Note that there should not be any need to normally disable this
     * feature: only consider that if there are actual perceived problems.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.1
     */
    EAGER_SERIALIZER_FETCH(true),

    /**
     * Feature that determines whether Object Identity is compared using
     * true JVM-level identity of Object (false); or, <code>equals()</code> method.
     * Latter is sometimes useful when dealing with Database-bound objects with
     * ORM libraries (like Hibernate). Note that Object itself is actually compared,
     * and NOT Object Id; naming of this feature is somewhat confusing, so it is important
     * that Object <b>for which identity is to be preserved</b> are considered equal,
     * above and beyond ids (which are always compared using equality anyway).
     *<p>
     * NOTE: due to the way functionality is implemented, it is very important that
     * in addition to overriding {@link Object#equals} for Objects to match (to be considered
     * "same") it is also necessary to ensure that {@link Object#hashCode()} is overridden
     * to produce the exact same value for equal instances.
     *<p>
     * Feature is disabled by default; meaning that strict identity is used, not
     * <code>equals()</code>
     *
     * @since 2.3
     */
    USE_EQUALITY_FOR_OBJECT_ID(false)
    ;

    private final boolean _defaultState;
    private final int _mask;

    private SerializationFeature(boolean defaultState) {
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

package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.cfg.ConfigFeature;

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
     * printer (see
     * {@link com.fasterxml.jackson.core.JsonGenerator#useDefaultPrettyPrinter}
     * for details).
     *<p>
     * Note that this only affects cases where
     * {@link com.fasterxml.jackson.core.JsonGenerator}
     * is constructed implicitly by ObjectMapper: if explicit
     * generator is passed, its configuration is not changed.
     *<p>
     * Also note that if you want to configure details of indentation,
     * you need to directly configure the generator: there is a
     * method to use any <code>PrettyPrinter</code> instance.
     * This feature will only allow using the default implementation.
     *<p>
     * Feature is enabled by default.
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
     * Feature that determines whether {@link java.util.Date} values
     * (and Date-based things like {@link java.util.Calendar}s) are to be
     * serialized as numeric timestamps (true; the default),
     * or as something else (usually textual representation).
     * If textual representation is used, the actual format is
     * one returned by a call to
     * {@link com.fasterxml.jackson.databind.SerializationConfig#getDateFormat}.
     *<p>
     * Note: whether this feature affects handling of other date-related
     * types depend on handlers of those types, although ideally they
     * should use this feature
     *<p>
     * Note: whether {@link java.util.Map} keys are serialized as Strings
     * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS}.
     *<p>
     * Feature is enabled by default.
     */
    WRITE_DATES_AS_TIMESTAMPS(true),

    /**
     * Feature that determines whether {@link java.util.Date}s
     * (and sub-types) used as {@link java.util.Map} keys are serialized
     * as timestamps or not (if not, will be serialized as textual
     * values).
     *<p>
     * Default value is 'false', meaning that Date-valued Map keys are serialized
     * as textual (ISO-8601) values.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

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
     * Feature that determines whethere Java Enum values are serialized
     * as numbers (true), or textual values (false). If textual values are
     * used, other settings are also considered.
     * If this feature is enabled,
     *  return value of <code>Enum.ordinal()</code>
     * (an integer) will be used as the serialization.
     *<p>
     * Note that this feature has precedence over {@link #WRITE_ENUMS_USING_TO_STRING},
     * which is only considered if this feature is set to false.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_ENUMS_USING_INDEX(false),
    
    /**
     * Feature that determines whether Map entries with null values are
     * to be serialized (true) or not (false).
     *<p>
     * For further details, check out [JACKSON-314]
     *<p>
     * Feature is enabled by default.
     */
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
     * Feature is enabled by default.
     */
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
     * <p>
     * Feature is disabled by default.
     */
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
     * This is the counterpart to {@link SerializationFeature#WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS}.
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
    EAGER_SERIALIZER_FETCH(true)    
    ;

    private final boolean _defaultState;
    
    private SerializationFeature(boolean defaultState) {
        _defaultState = defaultState;
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }

    @Override
    public int getMask() { return (1 << ordinal()); }
}

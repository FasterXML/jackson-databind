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
     * SerializationFeature that can be enabled to make root value (usually JSON
     * Object but can be any type) wrapped within a single property
     * JSON object, where key as the "root name", as determined by
     * annotation introspector (esp. for JAXB that uses
     * <code>@XmlRootElement.name</code>) or fallback (non-qualified
     * class name).
     * SerializationFeature is mostly intended for JAXB compatibility.
     *<p>
     * SerializationFeature is enabled by default.
     */
    WRAP_ROOT_VALUE(false),

    /**
     * SerializationFeature that allows enabling (or disabling) indentation
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
     * SerializationFeature is enabled by default.
     */
    INDENT_OUTPUT(false),
    
    /*
    /******************************************************
    /*  Error handling features
    /******************************************************
     */
    
    /**
     * SerializationFeature that determines what happens when no accessors are
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
     * SerializationFeature is enabled by default.
     */
    FAIL_ON_EMPTY_BEANS(true),

    /**
     * SerializationFeature that determines whether Jackson code should catch
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
     * SerializationFeature is enabled by default.
     */
    WRAP_EXCEPTIONS(true),

    /*
    /******************************************************
    /* Output life cycle features
    /******************************************************
     */
    
     /**
      * SerializationFeature that determines whether <code>close</code> method of
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
     * SerializationFeature is disabled by default.
      */
    CLOSE_CLOSEABLE(false),

    /**
     * SerializationFeature that determines whether <code>JsonGenerator.flush()</code> is
     * called after <code>writeValue()</code> method <b>that takes JsonGenerator
     * as an argument</b> completes (i.e. does NOT affect methods
     * that use other destinations); same for methods in {@link ObjectWriter}.
     * This usually makes sense; but there are cases where flushing
     * should not be forced: for example when underlying stream is
     * compressing and flush() causes compression state to be flushed
     * (which occurs with some compression codecs).
     *<p>
     * SerializationFeature is enabled by default.
     */
    FLUSH_AFTER_WRITE_VALUE(true),
     
    /*
    /******************************************************
    /* Data type - specific serialization configuration
    /******************************************************
     */

    /**
     * SerializationFeature that determines whether {@link java.util.Date} values
     * (and Date-based things like {@link java.util.Calendar}s) are to be
     * serialized as numeric timestamps (true; the default),
     * or as something else (usually textual representation).
     * If textual representation is used, the actual format is
     * one returned by a call to
     * {@link com.fasterxml.jackson.databind.cfg.SerializationConfig#getDateFormat}.
     *<p>
     * Note: whether this feature affects handling of other date-related
     * types depend on handlers of those types, although ideally they
     * should use this feature
     *<p>
     * Note: whether {@link java.util.Map} keys are serialized as Strings
     * or not is controlled using {@link #WRITE_DATE_KEYS_AS_TIMESTAMPS}.
     *<p>
     * SerializationFeature is enabled by default.
     */
    WRITE_DATES_AS_TIMESTAMPS(true),

    /**
     * SerializationFeature that determines whether {@link java.util.Date}s
     * (and sub-types) used as {@link java.util.Map} keys are serialized
     * as timestamps or not (if not, will be serialized as textual
     * values).
     *<p>
     * Default value is 'false', meaning that Date-valued Map keys are serialized
     * as textual (ISO-8601) values.
     *<p>
     * SerializationFeature is disabled by default.
     */
    WRITE_DATE_KEYS_AS_TIMESTAMPS(false),

    /**
     * SerializationFeature that determines how type <code>char[]</code> is serialized:
     * when enabled, will be serialized as an explict JSON array (with
     * single-character Strings as values); when disabled, defaults to
     * serializing them as Strings (which is more compact).
     *<p>
     * SerializationFeature is disabled by default.
     */
    WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS(false),

    /**
     * SerializationFeature that determines standard serialization mechanism used for
     * Enum values: if enabled, return value of <code>Enum.toString()</code>
     * is used; if disabled, return value of <code>Enum.name()</code> is used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link DeserializationFeature#READ_ENUMS_USING_TO_STRING}.
     *<p>
     * SerializationFeature is disabled by default.
     */
    WRITE_ENUMS_USING_TO_STRING(false),

    /**
     * SerializationFeature that determines whethere Java Enum values are serialized
     * as numbers (true), or textual values (false). If textual values are
     * used, other settings are also considered.
     * If this feature is enabled,
     *  return value of <code>Enum.ordinal()</code>
     * (an integer) will be used as the serialization.
     *<p>
     * Note that this feature has precedence over {@link #WRITE_ENUMS_USING_TO_STRING},
     * which is only considered if this feature is set to false.
     *<p>
     * SerializationFeature is disabled by default.
     */
    WRITE_ENUMS_USING_INDEX(false),
    
    /**
     * SerializationFeature that determines whether Map entries with null values are
     * to be serialized (true) or not (false).
     *<p>
     * For further details, check out [JACKSON-314]
     *<p>
     * SerializationFeature is enabled by default.
     */
    WRITE_NULL_MAP_VALUES(true),

    /**
     * SerializationFeature that determines whether Container properties (POJO properties
     * with declared value of Collection or array; i.e. things that produce JSON
     * arrays) that are empty (have no elements)
     * will be serialized as empty JSON arrays (true), or suppressed from output (false).
     *<p>
     * Note that this does not change behavior of {@link java.util.Map}s, or
     * "Collection-like" types.
     *<p>
     * SerializationFeature is enabled by default.
     */
    WRITE_EMPTY_JSON_ARRAYS(true)
    
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
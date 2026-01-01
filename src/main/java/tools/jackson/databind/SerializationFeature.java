package tools.jackson.databind;

import tools.jackson.databind.cfg.ConfigFeature;

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
    /**********************************************************************
    /* Generic output features
    /**********************************************************************
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
     * no explicit {@link tools.jackson.core.PrettyPrinter} has been configured
     * for the generator or {@link ObjectWriter}.
     *<p>
     * Feature is disabled by default.
     */
    INDENT_OUTPUT(false),

    /*
    /**********************************************************************
    /* Error handling features
    /**********************************************************************
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
     * Feature is disabled by default as of Jackson 3.0 (in 2.x it was enabled).
     */
    FAIL_ON_EMPTY_BEANS(false),

    /**
     * Feature that determines what happens when a direct self-reference
     * is detected by a POJO (and no Object Id handling is enabled for it):
     * either a {@link DatabindException} is
     * thrown (if true), or reference is normally processed (false).
     *<p>
     * Feature is enabled by default.
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
     */
    WRITE_SELF_REFERENCES_AS_NULL(false),

    /*
    /**********************************************************************
    /* Output life cycle features
    /**********************************************************************
     */

    /**
     * Feature that determines whether {@code close()} method of
     * serialized <b>root level</b> objects (ones for which {@link ObjectMapper}'s
     * (and {@link ObjectWriter}'s)
     * writeValue() (or equivalent) method is called)
     * that implement {@link java.lang.AutoCloseable}
     * is called after serialization or not. If enabled, <b>close()</b> will
     * be called after serialization completes (whether successfully, or
     * due to an error manifested by an exception being thrown). You can
     * think of this as sort of "finally" processing.
     *<p>
     * NOTE: only affects behavior with <b>root</b> objects, and not other
     * objects reachable from the root object. Put another way, only one
     * call will be made for each 'writeValue' call.
     *<p>
     * NOTE: This feature does not control closing of the underlying
     * {@link java.io.OutputStream} or {@link java.io.Writer}.
     * Stream closing is handled by the streaming layer and can instead be configured
     * via {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET}.
     *<p>
     * NOTE: in Jackson 2.x this used to only apply to {@link java.io.Closeable}s,
     * but now it also applies to {@link java.lang.AutoCloseable}s as well (as
     * of Jackson 3.0).
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
    /**********************************************************************
    /* Datatype-specific serialization configuration
    /**********************************************************************
     */

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
     * NOTE: Since 2.8 there are better mechanism for specifying filtering; specifically
     * using {@link com.fasterxml.jackson.annotation.JsonInclude} or configuration overrides.
     * This feature was deprecated from 2.8 through to 2.20 but no longer deprecated
     * since 2.21 / 3.0.
     *<p>
     * Feature is enabled by default.
     */
    WRITE_EMPTY_JSON_ARRAYS(true),

    /**
     * Feature added for inter-operability (originally to work with oddities of
     * so-called "BadgerFish" convention).
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
     * Feature that determines whether {@link java.util.Map} entries are first
     * sorted by key before serialization or not: if enabled, additional sorting
     * step is performed if necessary (not necessary for {@link java.util.SortedMap}s),
     * if disabled, no additional sorting is needed.
     *<p>
     * Feature is disabled by default.
     */
    ORDER_MAP_ENTRIES_BY_KEYS(false),

    /**
     * Feature that determines whether to intentionally fail when the mapper attempts to
     * order map entries with incomparable keys by accessing the first key of the map.
     * So depending on the Map implementation, this may not be the same key every time.
     * <p>
     * If enabled, will simply fail by throwing an exception.
     * If disabled, will not throw an exception and instead simply return the original map.
     * <p>
     * Note that this feature will apply only when configured to order map entries by keys, either
     * through annotation or enabling {@link #ORDER_MAP_ENTRIES_BY_KEYS}.
     * <p>
     * Feature is disabled by default.
     */
    FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY(false),

    /**
     * Feature that determines whether {@code JsonInclude#content()} configured
     * filtering is applied to elements of {@link java.util.Collection} and
     * array valued properties.
     * By default, {@code content()} inclusion rules are only applied to
     * {@code Map} values and reference type contents, and are ignored for
     * {@code Collection} and array elements.
     * <p>
     * When this feature is enabled, {@code JsonInclude#content()} rules
     * are used for filtering {@code Collection} and array elements during serialization.
     * <p>
     * This feature is <b>disabled by default</b> for backwards
     * compatibility.
     *
     * @since 3.1
     */
    APPLY_JSON_INCLUDE_FOR_CONTAINERS(false),

    /*
    /**********************************************************************
    /* Other
    /**********************************************************************
     */

    /**
     * Feature that determines whether {@link ObjectWriter} should
     * try to eagerly fetch necessary {@link ValueSerializer} when
     * possible. This improves performance in cases where similarly
     * configured {@link ObjectWriter} instance is used multiple
     * times; and should not significantly affect single-use cases.
     *<p>
     * Note that there should not be any need to normally disable this
     * feature: only consider that if there are actual perceived problems.
     *<p>
     * Feature is enabled by default.
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

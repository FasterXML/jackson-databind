package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.cfg.ConfigFeature;

/**
 * Enumeration that defines simple on/off features that affect
 * the way Java objects are deserialized from JSON
 *<p>
 * Note that features can be set both through
 * {@link ObjectMapper} (as sort of defaults) and through
 * {@link ObjectReader}.
 * In first case these defaults must follow "config-then-use" patterns
 * (i.e. defined once, not changed afterwards); all per-call
 * changes must be done using {@link ObjectReader}.
 */
public enum DeserializationFeature implements ConfigFeature
{
    /*
    /******************************************************
    /* Type conversion features
    /******************************************************
     */

    /**
     * SerializationFeature that determines whether JSON floating point numbers
     * are to be deserialized into {@link java.math.BigDecimal}s
     * if only generic type description (either {@link Object} or
     * {@link Number}, or within untyped {@link java.util.Map}
     * or {@link java.util.Collection} context) is available.
     * If enabled such values will be deserialized as {@link java.math.BigDecimal}s;
     * if disabled, will be deserialized as {@link Double}s.
     * <p>
     * SerializationFeature is disabled by default, meaning that "untyped" floating
     * point numbers will by default be deserialized as {@link Double}s
     * (choice is for performance reason -- BigDecimals are slower than
     * Doubles).
     */
    USE_BIG_DECIMAL_FOR_FLOATS(false),

    /**
     * SerializationFeature that determines whether JSON integral (non-floating-point)
     * numbers are to be deserialized into {@link java.math.BigInteger}s
     * if only generic type description (either {@link Object} or
     * {@link Number}, or within untyped {@link java.util.Map}
     * or {@link java.util.Collection} context) is available.
     * If enabled such values will be deserialized as
     * {@link java.math.BigInteger}s;
     * if disabled, will be deserialized as "smallest" available type,
     * which is either {@link Integer}, {@link Long} or
     * {@link java.math.BigInteger}, depending on number of digits.
     * <p>
     * SerializationFeature is disabled by default, meaning that "untyped" floating
     * point numbers will by default be deserialized using whatever
     * is the most compact integral type, to optimize efficiency.
     */
    USE_BIG_INTEGER_FOR_INTS(false),

    // [JACKSON-652]
    /**
     * SerializationFeature that determines whether JSON Array is mapped to
     * <code>Object[]</code> or <code>List&lt;Object></code> when binding
     * "untyped" objects (ones with nominal type of <code>java.lang.Object</code>).
     * If true, binds as <code>Object[]</code>; if false, as <code>List&lt;Object></code>.
     *<p>
     * SerializationFeature is disabled by default, meaning that JSON arrays are bound as
     * {@link java.util.List}s.
     */
    USE_JAVA_ARRAY_FOR_JSON_ARRAY(false),
    
    /**
     * SerializationFeature that determines standard deserialization mechanism used for
     * Enum values: if enabled, Enums are assumed to have been serialized  using
     * return value of <code>Enum.toString()</code>;
     * if disabled, return value of <code>Enum.name()</code> is assumed to have been used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link SerializationConfig.SerializationFeature#WRITE_ENUMS_USING_TO_STRING}.
     *<p>
     * SerializationFeature is disabled by default.
     */
    READ_ENUMS_USING_TO_STRING(false),
    
    /*
    /******************************************************
     *  Error handling features
    /******************************************************
     */

    /**
     * SerializationFeature that determines whether encountering of unknown
     * properties (ones that do not map to a property, and there is
     * no "any setter" or handler that can handle it)
     * should result in a failure (by throwing a
     * {@link JsonMappingException}) or not.
     * This setting only takes effect after all other handling
     * methods for unknown properties have been tried, and
     * property remains unhandled.
     *<p>
     * SerializationFeature is enabled by default (meaning that a
     * {@link JsonMappingException} will be thrown if an unknown property
     * is encountered).
     */
    FAIL_ON_UNKNOWN_PROPERTIES(true),

    /**
     * SerializationFeature that determines whether encountering of JSON null
     * is an error when deserializing into Java primitive types
     * (like 'int' or 'double'). If it is, a JsonProcessingException
     * is thrown to indicate this; if not, default value is used
     * (0 for 'int', 0.0 for double, same defaulting as what JVM uses).
     *<p>
     * SerializationFeature is disabled by default.
     */
    FAIL_ON_NULL_FOR_PRIMITIVES(false),

    /**
     * SerializationFeature that determines whether JSON integer numbers are valid
     * values to be used for deserializing Java enum values.
     * If set to 'false' numbers are acceptable and are used to map to
     * ordinal() of matching enumeration value; if 'true', numbers are
     * not allowed and a {@link JsonMappingException} will be thrown.
     * Latter behavior makes sense if there is concern that accidental
     * mapping from integer values to enums might happen (and when enums
     * are always serialized as JSON Strings)
     *<p>
     * SerializationFeature is disabled by default.
     */
    FAIL_ON_NUMBERS_FOR_ENUMS(false),

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
     * SerializationFeature is enabled by default.
     */
    WRAP_EXCEPTIONS(true),
    
    /*
    /******************************************************
     *  Structural conversion features
    /******************************************************
     */

    /**
     * SerializationFeature that determines whether it is acceptable to coerce non-array
     * (in JSON) values to work with Java collection (arrays, java.util.Collection)
     * types. If enabled, collection deserializers will try to handle non-array
     * values as if they had "implicit" surrounding JSON array.
     * This feature is meant to be used for compatibility/interoperability reasons,
     * to work with packages (such as XML-to-JSON converters) that leave out JSON
     * array in cases where there is just a single element in array.
     *<p>
     * SerializationFeature is disabled by default.
     */
    ACCEPT_SINGLE_VALUE_AS_ARRAY(false),
    
    /**
     * SerializationFeature to allow "unwrapping" root-level JSON value, to match setting of
     * {@link SerializationConfig.SerializationFeature#WRAP_ROOT_VALUE} used for serialization.
     * Will verify that the root JSON value is a JSON Object, and that it has
     * a single property with expected root name. If not, a
     * {@link JsonMappingException} is thrown; otherwise value of the wrapped property
     * will be deserialized as if it was the root value.
     *<p>
     * SerializationFeature is disabled by default.
     */
    UNWRAP_ROOT_VALUE(false),

    /*
    /******************************************************
    /* Value conversion features
    /******************************************************
     */
    
    /**
     * SerializationFeature that can be enabled to allow JSON empty String
     * value ("") to be bound to POJOs as null.
     * If disabled, standard POJOs can only be bound from JSON null or
     * JSON Object (standard meaning that no custom deserializers or
     * constructors are defined; both of which can add support for other
     * kinds of JSON values); if enable, empty JSON String can be taken
     * to be equivalent of JSON null.
     *<p>
     * SerializationFeature is enabled by default.
     */
    ACCEPT_EMPTY_STRING_AS_NULL_OBJECT(false)
    
    ;

    private final boolean _defaultState;
    
    private DeserializationFeature(boolean defaultState) {
        _defaultState = defaultState;
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }

    @Override
    public int getMask() { return (1 << ordinal()); }
}
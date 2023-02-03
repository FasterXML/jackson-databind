package com.fasterxml.jackson.databind;

import com.fasterxml.jackson.databind.cfg.ConfigFeature;
import com.fasterxml.jackson.databind.exc.InvalidNullException;

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
 *<p>
 * Note that features that do not indicate version of inclusion
 * were available in Jackson 2.0 (or earlier); only later additions
 * indicate version of inclusion.
 */
public enum DeserializationFeature implements ConfigFeature
{
    /*
    /******************************************************
    /* Value (mostly scalar) conversion features
    /******************************************************
     */

    /**
     * Feature that determines whether JSON floating point numbers
     * are to be deserialized into {@link java.math.BigDecimal}s
     * if only generic type description (either {@link Object} or
     * {@link Number}, or within untyped {@link java.util.Map}
     * or {@link java.util.Collection} context) is available.
     * If enabled such values will be deserialized as {@link java.math.BigDecimal}s;
     * if disabled, will be deserialized as {@link Double}s.
     *<p>
     * NOTE: one aspect of {@link java.math.BigDecimal} handling that may need
     * configuring is whether trailing zeroes are trimmed:
     * {@link com.fasterxml.jackson.databind.node.JsonNodeFactory} has
     * {@link com.fasterxml.jackson.databind.node.JsonNodeFactory#withExactBigDecimals} for
     * changing default behavior (default is for trailing zeroes to be trimmed).
     *<p>
     * Feature is disabled by default, meaning that "untyped" floating
     * point numbers will by default be deserialized as {@link Double}s
     * (choice is for performance reason -- BigDecimals are slower than
     * Doubles).
     */
    USE_BIG_DECIMAL_FOR_FLOATS(false),

    /**
     * Feature that determines whether JSON integral (non-floating-point)
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
     * Feature is disabled by default, meaning that "untyped" integral
     * numbers will by default be deserialized using whatever
     * is the most compact integral type, to optimize efficiency.
     */
    USE_BIG_INTEGER_FOR_INTS(false),

    /**
     * Feature that determines how "small" JSON integral (non-floating-point)
     * numbers -- ones that fit in 32-bit signed integer (`int`) -- are bound
     * when target type is loosely typed as {@link Object} or {@link Number}
     * (or within untyped {@link java.util.Map} or {@link java.util.Collection} context).
     * If enabled, such values will be deserialized as {@link java.lang.Long};
     * if disabled, they will be deserialized as "smallest" available type,
     * {@link Integer}.
     *<p>
     * Note: if {@link #USE_BIG_INTEGER_FOR_INTS} is enabled, it has precedence
     * over this setting, forcing use of {@link java.math.BigInteger} for all
     * integral values.
     *<p>
     * Feature is disabled by default, meaning that "untyped" integral
     * numbers will by default be deserialized using {@link java.lang.Integer}
     * if value fits.
     *
     * @since 2.6
     */
    USE_LONG_FOR_INTS(false),

    /**
     * Feature that determines whether JSON Array is mapped to
     * <code>Object[]</code> or {@code List<Object>} when binding
     * "untyped" objects (ones with nominal type of <code>java.lang.Object</code>).
     * If true, binds as <code>Object[]</code>; if false, as {@code List<Object>}.
     *<p>
     * Feature is disabled by default, meaning that JSON arrays are bound as
     * {@link java.util.List}s.
     */
    USE_JAVA_ARRAY_FOR_JSON_ARRAY(false),

    /*
    /******************************************************
    /* Error handling features
    /******************************************************
     */

    /**
     * Feature that determines whether encountering of unknown
     * properties (ones that do not map to a property, and there is
     * no "any setter" or handler that can handle it)
     * should result in a failure (by throwing a
     * {@link JsonMappingException}) or not.
     * This setting only takes effect after all other handling
     * methods for unknown properties have been tried, and
     * property remains unhandled.
     *<p>
     * Feature is enabled by default (meaning that a
     * {@link JsonMappingException} will be thrown if an unknown property
     * is encountered).
     */
    FAIL_ON_UNKNOWN_PROPERTIES(true),

    /**
     * Feature that determines whether encountering of JSON null
     * is an error when deserializing into Java primitive types
     * (like 'int' or 'double'). If it is, a {@link InvalidNullException}
     * is thrown to indicate this; if not, default value is used
     * (0 for 'int', 0.0 for double, same defaulting as what JVM uses).
     *<p>
     * Feature is disabled by default.
     */
    FAIL_ON_NULL_FOR_PRIMITIVES(false),

    /**
     * Feature that determines whether JSON integer numbers are valid
     * values to be used for deserializing Java enum values.
     * If set to 'false' numbers are acceptable and are used to map to
     * ordinal() of matching enumeration value; if 'true', numbers are
     * not allowed and a {@link JsonMappingException} will be thrown.
     * Latter behavior makes sense if there is concern that accidental
     * mapping from integer values to enums might happen (and when enums
     * are always serialized as JSON Strings)
     *<p>
     * Feature is disabled by default.
     */
    FAIL_ON_NUMBERS_FOR_ENUMS(false),

    /**
     * Feature that determines what happens when type of a polymorphic
     * value (indicated for example by {@link com.fasterxml.jackson.annotation.JsonTypeInfo})
     * cannot be found (missing) or resolved (invalid class name, non-mappable id);
     * if enabled, an exception is thrown; if false, null value is used instead.
     *<p>
     * Feature is enabled by default so that exception is thrown for missing or invalid
     * type information.
     *
     * @since 2.2
     */
    FAIL_ON_INVALID_SUBTYPE(true),

    /**
     * Feature that determines what happens when reading JSON content into tree
     * ({@link com.fasterxml.jackson.core.TreeNode}) and a duplicate key
     * is encountered (property name that was already seen for the JSON Object).
     * If enabled, {@link JsonMappingException} will be thrown; if disabled, no exception
     * is thrown and the new (later) value overwrites the earlier value.
     *<p>
     * Note that this property does NOT affect other aspects of data-binding; that is,
     * no detection is done with respect to POJO properties or {@link java.util.Map}
     * keys. New features may be added to control additional cases.
     *<p>
     * Feature is disabled by default so that no exception is thrown.
     *
     * @since 2.3
     */
    FAIL_ON_READING_DUP_TREE_KEY(false),

    /**
     * Feature that determines what happens when a property that has been explicitly
     * marked as ignorable is encountered in input: if feature is enabled,
     * {@link JsonMappingException} is thrown; if false, property is quietly skipped.
     *<p>
     * Feature is disabled by default so that no exception is thrown.
     *
     * @since 2.3
     */
    FAIL_ON_IGNORED_PROPERTIES(false),

    /**
     * Feature that determines what happens if an Object Id reference is encountered
     * that does not refer to an actual Object with that id ("unresolved Object Id"):
     * either an exception is thrown (<code>true</code>), or a null object is used
     * instead (<code>false</code>).
     * Note that if this is set to <code>false</code>, no further processing is done;
     * specifically, if reference is defined via setter method, that method will NOT
     * be called.
     *<p>
     * Feature is enabled by default, so that unknown Object Ids will result in an
     * exception being thrown, at the end of deserialization.
     *
     * @since 2.5
     */
    FAIL_ON_UNRESOLVED_OBJECT_IDS(true),

    /**
     * Feature that determines what happens if one or more Creator properties (properties
     * bound to parameters of Creator method (constructor or static factory method))
     * are missing value to bind to from content.
     * If enabled, such missing values result in a {@link JsonMappingException} being
     * thrown with information on the first one (by index) of missing properties.
     * If disabled, and if property is NOT marked as required,
     * missing Creator properties are filled
     * with <code>null values</code> provided by deserializer for the type of parameter
     * (usually null for Object types, and default value for primitives; but redefinable
     * via custom deserializers).
     *<p>
     * Note that having an injectable value counts as "not missing".
     *<p>
     * Feature is disabled by default, so that no exception is thrown for missing creator
     * property values, unless they are explicitly marked as `required`.
     *
     * @since 2.6
     */
    FAIL_ON_MISSING_CREATOR_PROPERTIES(false),

    /**
      * Feature that determines what happens if one or more Creator properties (properties
      * bound to parameters of Creator method (constructor or static factory method))
      * are bound to null values - either from the JSON or as a default value. This
      * is useful if you want to avoid nulls in your codebase, and particularly useful
      * if you are using Java or Scala optionals for non-mandatory fields.
      * Feature is disabled by default, so that no exception is thrown for missing creator
      * property values, unless they are explicitly marked as `required`.
      *
      * @since 2.8
      */
    FAIL_ON_NULL_CREATOR_PROPERTIES(false),

    /**
     * Feature that determines what happens when a property annotated with
     * {@link com.fasterxml.jackson.annotation.JsonTypeInfo.As#EXTERNAL_PROPERTY} is missing,
     * but associated type id is available. If enabled, {@link JsonMappingException} is always
     * thrown when property value is missing (if type id does exist);
     * if disabled, exception is only thrown if property is marked as `required`.
     *<p>
     * Feature is enabled by default, so that exception is thrown when a subtype property is
     * missing.
     *
     * @since 2.9
     */
    FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY(true),

    /**
     * Feature that determines behaviour for data-binding after binding the root value.
     * If feature is enabled, one more call to
     * {@link com.fasterxml.jackson.core.JsonParser#nextToken} is made to ensure that
     * no more tokens are found (and if any is found,
     * {@link com.fasterxml.jackson.databind.exc.MismatchedInputException} is thrown); if
     * disabled, no further checks are made.
     *<p>
     * Feature could alternatively be called <code>READ_FULL_STREAM</code>, since it
     * effectively verifies that input stream contains only as much data as is needed
     * for binding the full value, and nothing more (except for possible ignorable
     * white space or comments, if supported by data format).
     *<p>
     * Feature is disabled by default (so that no check is made for possible trailing
     * token(s)) for backwards compatibility reasons.
     *
     * @since 2.9
     */
    FAIL_ON_TRAILING_TOKENS(false),

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
     * NOTE: most of the time exceptions that may or may not be wrapped are of
     * type {@link RuntimeException}: as mentioned earlier, various
     * {@link java.io.IOException}s (and in particular
     * {@link com.fasterxml.jackson.core.JacksonException}s) will
     * always be passed as-is.
     *<p>
     * Feature is enabled by default.
     */
    WRAP_EXCEPTIONS(true),

    /*
    /******************************************************
    /* Structural conversion features
    /******************************************************
     */

    /**
     * Feature that determines whether it is acceptable to coerce non-array
     * (in JSON) values to work with Java collection (arrays, java.util.Collection)
     * types. If enabled, collection deserializers will try to handle non-array
     * values as if they had "implicit" surrounding JSON array.
     * This feature is meant to be used for compatibility/interoperability reasons,
     * to work with packages (such as XML-to-JSON converters) that leave out JSON
     * array in cases where there is just a single element in array.
     *<p>
     * Feature is disabled by default.
     */
    ACCEPT_SINGLE_VALUE_AS_ARRAY(false),

    /**
     * Feature that determines whether it is acceptable to coerce single value array (in JSON)
     * values to the corresponding value type.  This is basically the opposite of the {@link #ACCEPT_SINGLE_VALUE_AS_ARRAY}
     * feature.  If more than one value is found in the array, a JsonMappingException is thrown.
     * <p>
     * NOTE: only <b>single</b> wrapper Array is allowed: if multiple attempted, exception
     * will be thrown.
     *
     * Feature is disabled by default.
     * @since 2.4
     */
    UNWRAP_SINGLE_VALUE_ARRAYS(false),

    /**
     * Feature to allow "unwrapping" root-level JSON value, to match setting of
     * {@link SerializationFeature#WRAP_ROOT_VALUE} used for serialization.
     * Will verify that the root JSON value is a JSON Object, and that it has
     * a single property with expected root name. If not, a
     * {@link JsonMappingException} is thrown; otherwise value of the wrapped property
     * will be deserialized as if it was the root value.
     *<p>
     * Feature is disabled by default.
     */
    UNWRAP_ROOT_VALUE(false),

    /*
    /******************************************************
    /* Value conversion features
    /******************************************************
     */

    /**
     * Feature that can be enabled to allow JSON empty String
     * value ("") to be bound as `null` for POJOs and other structured
     * values ({@link java.util.Map}s, {@link java.util.Collection}s).
     * If disabled, standard POJOs can only be bound from JSON `null` or
     * JSON Object (standard meaning that no custom deserializers or
     * constructors are defined; both of which can add support for other
     * kinds of JSON values); if enabled, empty JSON String can be taken
     * to be equivalent of JSON null.
     *<p>
     * NOTE: this does NOT apply to scalar values such as booleans and numbers;
     * whether they can be coerced depends on
     * {@link MapperFeature#ALLOW_COERCION_OF_SCALARS}.
     *<p>
     * Feature is disabled by default.
     */
    ACCEPT_EMPTY_STRING_AS_NULL_OBJECT(false),

    /**
     * Feature that can be enabled to allow empty JSON Array
     * value (that is, <code>[ ]</code>) to be bound to POJOs (and
     * with 2.9, other values too) as `null`.
     * If disabled, standard POJOs can only be bound from JSON `null` or
     * JSON Object (standard meaning that no custom deserializers or
     * constructors are defined; both of which can add support for other
     * kinds of JSON values); if enabled, empty JSON Array will be taken
     * to be equivalent of JSON null.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.5
     */
    ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT(false),

    /**
     * Feature that determines whether coercion from JSON floating point
     * number (anything with command (`.`) or exponent portion (`e` / `E'))
     * to an expected integral number (`int`, `long`, `java.lang.Integer`, `java.lang.Long`,
     * `java.math.BigDecimal`) is allowed or not.
     * If enabled, coercion truncates value; if disabled, a {@link JsonMappingException}
     * will be thrown.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.6
     */
    ACCEPT_FLOAT_AS_INT(true),

    /**
     * Feature that determines standard deserialization mechanism used for
     * Enum values: if enabled, Enums are assumed to have been serialized  using
     * return value of <code>Enum.toString()</code>;
     * if disabled, return value of <code>Enum.name()</code> is assumed to have been used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link SerializationFeature#WRITE_ENUMS_USING_TO_STRING}.
     *<p>
     * Feature is disabled by default.
     */
    READ_ENUMS_USING_TO_STRING(false),

    /**
     * Feature that allows unknown Enum values to be parsed as null values.
     * If disabled, unknown Enum values will throw exceptions.
     *<p>
     * Note that in some cases this will in effect ignore unknown {@code Enum} values,
     * e.g. when the unknown values are used as keys of {@link java.util.EnumMap}
     * or values of {@link java.util.EnumSet}: this because these data structures cannot
     * store {@code null} values.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.0
     */
    READ_UNKNOWN_ENUM_VALUES_AS_NULL(false),

    /**
     * Feature that allows unknown Enum values to be ignored and a predefined value specified through
     * {@link com.fasterxml.jackson.annotation.JsonEnumDefaultValue @JsonEnumDefaultValue} annotation.
     * If disabled, unknown Enum values will throw exceptions.
     * If enabled, but no predefined default Enum value is specified, an exception will be thrown as well.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.8
     */
    READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE(false),

    /**
     * Feature that controls whether numeric timestamp values are expected
     * to be written using nanosecond timestamps (enabled) or not (disabled),
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
    READ_DATE_TIMESTAMPS_AS_NANOSECONDS(true),

    /**
     * Feature that specifies whether context provided {@link java.util.TimeZone}
     * ({@link DeserializationContext#getTimeZone()} should be used to adjust Date/Time
     * values on deserialization, even if value itself contains timezone information.
     * If enabled, contextual <code>TimeZone</code> will essentially override any other
     * TimeZone information; if disabled, it will only be used if value itself does not
     * contain any TimeZone information.
     *<p>
     * Note that exact behavior depends on date/time types in question; and specifically
     * JDK type of {@link java.util.Date} does NOT have in-built timezone information
     * so this setting has no effect.
     * Further, while {@link java.util.Calendar} does have this information basic
     * JDK {@link java.text.SimpleDateFormat} is unable to retain parsed zone information,
     * and as a result, {@link java.util.Calendar} will always get context timezone
     * adjustment regardless of this setting.
     *<p>
     *<p>
     * Taking above into account, this feature is supported only by extension modules for
     * Joda and Java 8 date/time datatypes.
     *
     * @since 2.2
     */
    ADJUST_DATES_TO_CONTEXT_TIME_ZONE(true),

    /*
    /******************************************************
    /* Other
    /******************************************************
     */

    /**
     * Feature that determines whether {@link ObjectReader} should
     * try to eagerly fetch necessary {@link JsonDeserializer} when
     * possible. This improves performance in cases where similarly
     * configured {@link ObjectReader} instance is used multiple
     * times; and should not significantly affect single-use cases.
     *<p>
     * Note that there should not be any need to normally disable this
     * feature: only consider that if there are actual perceived problems.
     *<p>
     * Feature is enabled by default.
     *
     * @since 2.1
     */
    EAGER_DESERIALIZER_FETCH(true)

    ;

    private final boolean _defaultState;
    private final int _mask;

    private DeserializationFeature(boolean defaultState) {
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

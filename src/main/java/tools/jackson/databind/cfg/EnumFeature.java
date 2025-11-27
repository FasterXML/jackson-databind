package tools.jackson.databind.cfg;

import tools.jackson.databind.DatabindException;

/**
 * New Datatype-specific configuration options related to handling of
 * {@link java.lang.Enum} types.
 */
public enum EnumFeature implements DatatypeFeature
{
    /*
    /**********************************************************************
    /* READ
    /**********************************************************************
     */

    /**
     * Feature that determines standard deserialization mechanism used for
     * Enum values: if enabled, Enums are assumed to have been serialized using
     * index of <code>Enum</code>;
     *<p>
     * Note: this feature should be symmetric to
     * as {@link #WRITE_ENUM_KEYS_USING_INDEX}.
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.15
     */
    READ_ENUM_KEYS_USING_INDEX(false),

    /**
     * Feature that determines whether JSON integer numbers are valid
     * values to be used for deserializing Java enum values.
     * If set to 'false' numbers are acceptable and are used to map to
     * ordinal() of matching enumeration value; if 'true', numbers are
     * not allowed and a {@link DatabindException} will be thrown.
     * Latter behavior makes sense if there is concern that accidental
     * mapping from integer values to enums might happen (and when enums
     * are always serialized as JSON Strings)
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.DeserializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default.
     */
    FAIL_ON_NUMBERS_FOR_ENUMS(false),

    /**
     * Feature that determines the deserialization mechanism used for
     * Enum values: if enabled, Enums are assumed to have been serialized using
     * return value of {@code Enum.toString()};
     * if disabled, return value of {@code Enum.name()} is assumed to have been used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link #WRITE_ENUMS_USING_TO_STRING}.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.DeserializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is enabled by default as of Jackson 3.0 (in 2.x it was disabled).
     */
    READ_ENUMS_USING_TO_STRING(true),

    /**
     * Feature that allows unknown Enum values to be parsed as {@code null} values.
     * If disabled, unknown Enum values will throw exceptions.
     * <p>
     * Note that in some cases this will effectively ignore unknown {@code Enum} values,
     * e.g. when the unknown values are used as keys of {@link java.util.EnumMap}
     * or values of {@link java.util.EnumSet}: this is because these data structures cannot
     * store {@code null} values.
     * <p>
     * Also note that this feature has lower precedence than
     * {@link #READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE},
     * meaning this feature will work only if latter feature is disabled.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.DeserializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default.
     */
    READ_UNKNOWN_ENUM_VALUES_AS_NULL(false),

    /**
     * Feature that allows unknown Enum values to be ignored and replaced by a predefined value specified through
     * {@link com.fasterxml.jackson.annotation.JsonEnumDefaultValue @JsonEnumDefaultValue} annotation.
     * If disabled, unknown Enum values will throw exceptions.
     * If enabled, but no predefined default Enum value is specified, an exception will be thrown as well.
     * <p>
     * Note that this feature has higher precedence than {@link #READ_UNKNOWN_ENUM_VALUES_AS_NULL}.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.DeserializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default.
     */
    READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE(false),

    /*
    /**********************************************************************
    /* WRITE
    /**********************************************************************
     */


    /**
     * Feature that determines standard serialization mechanism used for
     * Enum values: if enabled, return value of <code>Enum.name().toLowerCase()</code>
     * is used; if disabled, return value of <code>Enum.name()</code> is used.
     *<p>
     * NOTE: this feature CANNOT be changed on per-call basis: it will have to
     * be set on {@code ObjectMapper} before use
     *<p>
     * Feature is disabled by default.
     *
     * @since 2.15
     */
    WRITE_ENUMS_TO_LOWERCASE(false),

    /**
     * Feature that determines standard serialization mechanism used for
     * Enum values: if enabled, return value of <code>Enum.toString()</code>
     * is used; if disabled, return value of <code>Enum.name()</code> is used.
     *<p>
     * Note: this feature should usually have same value
     * as {@link #READ_ENUMS_USING_TO_STRING}.
     *<p>
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is enabled by default as of Jackson 3.0 (in 2.x it was disabled).
     */
    WRITE_ENUMS_USING_TO_STRING(true),

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
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
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
     * Feature used to be one of {@link tools.jackson.databind.SerializationFeature}s
     * in Jackson 2.x but was moved here in 3.0.
     *<p>
     * Feature is disabled by default.
     */
    WRITE_ENUM_KEYS_USING_INDEX(false),
    ;

    private final static int FEATURE_INDEX = DatatypeFeatures.FEATURE_INDEX_ENUM;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _enabledByDefault;

    private final int _mask;

    private EnumFeature(boolean enabledByDefault) {
        _enabledByDefault = enabledByDefault;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _enabledByDefault; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }

    @Override
    public int featureIndex() {
        return FEATURE_INDEX;
    }
}

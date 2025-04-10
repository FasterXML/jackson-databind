package tools.jackson.databind.cfg;

import tools.jackson.databind.DeserializationFeature;

/**
 * New Datatype-specific configuration options related to handling of
 * {@link java.lang.Enum} types.
 */
public enum EnumFeature implements DatatypeFeature
{
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
     * Feature that determines standard serialization mechanism used for
     * Enum values: if enabled, return value of <code>Enum.name().toLowerCase()</code>
     * is used; if disabled, return value of <code>Enum.name()</code> is used.
     *<p>
     * NOTE: this feature CAN NOT be changed on per-call basis: it will have to
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
     * as {@link DeserializationFeature#READ_ENUMS_USING_TO_STRING}.
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

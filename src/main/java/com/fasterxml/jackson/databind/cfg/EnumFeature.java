package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * New Datatype-specific configuration options related to handling of
 * {@link java.lang.Enum} types.
 *
 * @since 2.14
 */
public enum EnumFeature implements DatatypeFeature
{
    /**
     * Feature that determines standard deserialization mechanism used for
     * Enum values: if enabled, Enums are assumed to have been serialized using
     * index of <code>Enum</code>;
     *<p>
     * Note: this feature should be symmetric to
     * as {@link SerializationFeature#WRITE_ENUM_KEYS_USING_INDEX}.
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
    WRITE_ENUMS_TO_LOWERCASE(false);

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

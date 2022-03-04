package com.fasterxml.jackson.databind.cfg;

public enum EnumFeature implements DataTypeFeature
{
    BOGUS_FEATURE(false);

    private final static int FEATURE_INDEX = DataTypeFeatures.FEATURE_INDEX_ENUM;

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

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.core.FormatFeature;

public enum BogusFormatFeature
    implements FormatFeature
{
    FF_ENABLED_BY_DEFAULT(true),
    FF_DISABLED_BY_DEFAULT(false);

    private boolean _default;

    private BogusFormatFeature(boolean d) {
        _default = d;
    }

    @Override
    public boolean enabledByDefault() {
        return _default;
    }

    @Override
    public int getMask() {
        return (1 << ordinal());
    }

    @Override
    public boolean enabledIn(int flags) {
        return (flags & getMask()) != 0;
    }
}

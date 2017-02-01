package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Extension of {@link ConfigOverride} that allows changing of
 * contained configuration settings. Exposed to
 * {@link com.fasterxml.jackson.databind.Module}s that want to set
 * overrides, but not exposed to functionality that wants to apply
 * overrides.
 *
 * @since 2.8
 */
public class MutableConfigOverride
    extends ConfigOverride
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public MutableConfigOverride() { super(); }

    protected MutableConfigOverride(MutableConfigOverride src) {
        super(src);
    }

    public MutableConfigOverride copy() {
        return new MutableConfigOverride(this);
    }

    public MutableConfigOverride setFormat(JsonFormat.Value v) {
        _format = v;
        return this;
    }

    public MutableConfigOverride setInclude(JsonInclude.Value v) {
        _include = v;
        return this;
    }

    public MutableConfigOverride setIgnorals(JsonIgnoreProperties.Value v) {
        _ignorals = v;
        return this;
    }

    public MutableConfigOverride setIsIgnoredType(Boolean v) {
        _isIgnoredType = v;
        return this;
    }

    /**
     * @since 2.9
     */
    public MutableConfigOverride setSetterInfo(JsonSetter.Value v) {
        _setterInfo = v;
        return this;
    }

    /**
     * @since 2.9
     */
    public MutableConfigOverride setVisibility(JsonAutoDetect.Value v) {
        _visibility = v;
        return this;
    }

    /**
     * @since 2.9
     */
    public MutableConfigOverride setMergeable(Boolean v) {
        _mergeable = v;
        return this;
    }
}

package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Extension of {@link PropertyConfigOverride} that allows changing of
 * contained configuration settings. Exposed to
 * {@link com.fasterxml.jackson.databind.Module}s that want to set
 * overrides, but not exposed to functionality that wants to apply
 * overrides.
 *
 * @since 2.8
 */
public class MutablePropertyConfigOverride
    extends PropertyConfigOverride
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public MutablePropertyConfigOverride() { super(); }

    protected MutablePropertyConfigOverride(MutablePropertyConfigOverride src) {
        super(src);
    }
    
    public MutablePropertyConfigOverride copy() {
        return new MutablePropertyConfigOverride(this);
    }

    public MutablePropertyConfigOverride setFormat(JsonFormat.Value v) {
        _format = v;
        return this;
    }
    
    public MutablePropertyConfigOverride getInclude(JsonInclude.Value v) {
        _include = v;
        return this;
    }
}

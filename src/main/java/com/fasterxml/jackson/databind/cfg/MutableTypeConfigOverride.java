package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Extension of {@link TypeConfigOverride} that allows changing of
 * contained configuration settings. Exposed to
 * {@link com.fasterxml.jackson.databind.Module}s that want to set
 * overrides, but not exposed to functionality that wants to apply
 * overrides.
 *
 * @since 2.8
 */
public class MutableTypeConfigOverride
    extends TypeConfigOverride
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public MutableTypeConfigOverride() { super(); }

    protected MutableTypeConfigOverride(MutableTypeConfigOverride src) {
        super(src);
    }
    
    protected MutableTypeConfigOverride copy() {
        return new MutableTypeConfigOverride(this);
    }

    public MutableTypeConfigOverride setFormat(JsonFormat.Value v) {
        _format = v;
        return this;
    }
    
    public MutableTypeConfigOverride setInclude(JsonInclude.Value v) {
        _include = v;
        return this;
    }

    public MutableTypeConfigOverride setIsIgnoredType(Boolean v) {
        _isIgnoredType = v;
        return this;
    }
}

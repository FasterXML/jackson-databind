package com.fasterxml.jackson.databind.cfg;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

/**
 * Container for individual {@link ConfigOverride} values.
 */
public class ConfigOverrides
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Convenience value used as the default root setting.
     *
     * @since 3.0
     */
    public final static JsonInclude.Value INCLUDE_ALL
        = JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.ALWAYS);
    
    /**
     * Per-type override definitions
     */
    protected Map<Class<?>, MutableConfigOverride> _overrides;

    // // // Global defaulting

    protected JsonInclude.Value _defaultInclusion;

    protected JsonSetter.Value _defaultSetterInfo;

    protected VisibilityChecker<?> _visibilityChecker;

    protected Boolean _defaultMergeable;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

    public ConfigOverrides() {
        this(null,
                INCLUDE_ALL,
                JsonSetter.Value.empty(),
                VisibilityChecker.Std.defaultInstance(),
                null
        );
    }

    protected ConfigOverrides(Map<Class<?>, MutableConfigOverride> overrides,
            JsonInclude.Value defIncl,
            JsonSetter.Value defSetter,
            VisibilityChecker<?> defVisibility,
            Boolean defMergeable) {
        _overrides = overrides;
        _defaultInclusion = defIncl;
        _defaultSetterInfo = defSetter;
        _visibilityChecker = defVisibility;
        _defaultMergeable = defMergeable;
    }

    public ConfigOverrides copy()
    {
        Map<Class<?>, MutableConfigOverride> newOverrides;
        if (_overrides == null) {
            newOverrides = null;
        } else {
            newOverrides = _newMap();
            for (Map.Entry<Class<?>, MutableConfigOverride> entry : _overrides.entrySet()) {
                newOverrides.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return new ConfigOverrides(newOverrides,
                _defaultInclusion, _defaultSetterInfo, _visibilityChecker, _defaultMergeable);
    }

    /*
    /**********************************************************
    /* Per-type override access
    /**********************************************************
     */
    
    public ConfigOverride findOverride(Class<?> type) {
        if (_overrides == null) {
            return null;
        }
        return _overrides.get(type);
    }

    public MutableConfigOverride findOrCreateOverride(Class<?> type) {
        if (_overrides == null) {
            _overrides = _newMap();
        }
        MutableConfigOverride override = _overrides.get(type);
        if (override == null) {
            override = new MutableConfigOverride();
            _overrides.put(type, override);
        }
        return override;
    }

    /*
    /**********************************************************
    /* Global defaults accessors
    /**********************************************************
     */

    public JsonInclude.Value getDefaultInclusion() {
        return _defaultInclusion;
    }

    public JsonSetter.Value getDefaultSetterInfo() {
        return _defaultSetterInfo;
    }

    public Boolean getDefaultMergeable() {
        return _defaultMergeable;
    }

    public VisibilityChecker<?> getDefaultVisibility() {
        return _visibilityChecker;
    }

    /*
    /**********************************************************
    /* Global defaults mutators
    /**********************************************************
     */

    public ConfigOverrides setDefaultInclusion(JsonInclude.Value v) {
        _defaultInclusion = v;
        return this;
    }

    public ConfigOverrides setDefaultSetterInfo(JsonSetter.Value v) {
        _defaultSetterInfo = v;
        return this;
    }

    public ConfigOverrides setDefaultMergeable(Boolean v) {
        _defaultMergeable = v;
        return this;
    }

    public ConfigOverrides setDefaultVisibility(VisibilityChecker<?> v) {
        _visibilityChecker = v;
        return this;
    }

    public ConfigOverrides setDefaultVisibility(JsonAutoDetect.Value vis) {
        _visibilityChecker = VisibilityChecker.Std.construct(vis);
        return this;
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected Map<Class<?>, MutableConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutableConfigOverride>();
    }
}

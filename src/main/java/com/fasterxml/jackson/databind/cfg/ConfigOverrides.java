package com.fasterxml.jackson.databind.cfg;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

/**
 * Container for individual {@link ConfigOverride} values.
 *
 * @since 2.8
 */
public class ConfigOverrides
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Per-type override definitions
     */
    protected Map<Class<?>, MutableConfigOverride> _overrides;

    // // // Global defaulting

    /**
     * @since 2.9
     */
    protected JsonInclude.Value _defaultInclusion;

    /**
     * @since 2.9
     */
    protected JsonSetter.Value _defaultSetterInfo;

    /**
     * @since 2.9
     */
    protected VisibilityChecker<?> _visibilityChecker;

    /**
     * @since 2.9
     */
    protected Boolean _defaultMergeable;

    /**
     * Global default setting (if any) for leniency: if disabled ({link Boolean#TRUE}),
     * "strict" (not lenient): default setting if absence of value is considered "lenient"
     * in Jackson 2.x. Default setting may be overridden by per-type and per-property
     * settings.
     *
     * @since 2.10
     */
    protected Boolean _defaultLeniency;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public ConfigOverrides() {
        this(null,
                // !!! TODO: change to (ALWAYS, ALWAYS)?
                JsonInclude.Value.empty(),
                JsonSetter.Value.empty(),
                VisibilityChecker.Std.defaultInstance(),
                null, null
        );
    }

    /**
     * @since 2.10
     */
    protected ConfigOverrides(Map<Class<?>, MutableConfigOverride> overrides,
            JsonInclude.Value defIncl, JsonSetter.Value defSetter,
            VisibilityChecker<?> defVisibility, Boolean defMergeable, Boolean defLeniency)
    {
        _overrides = overrides;
        _defaultInclusion = defIncl;
        _defaultSetterInfo = defSetter;
        _visibilityChecker = defVisibility;
        _defaultMergeable = defMergeable;
        _defaultLeniency = defLeniency;
    }

    /**
     * @deprecated Since 2.10
     */
    @Deprecated // since 2.10
    protected ConfigOverrides(Map<Class<?>, MutableConfigOverride> overrides,
            JsonInclude.Value defIncl, JsonSetter.Value defSetter,
            VisibilityChecker<?> defVisibility, Boolean defMergeable) {
        this(overrides, defIncl, defSetter, defVisibility, defMergeable, null);
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
                _defaultInclusion, _defaultSetterInfo, _visibilityChecker,
                _defaultMergeable, _defaultLeniency);
    }

    /*
    /**********************************************************************
    /* Per-type override access
    /**********************************************************************
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

    /**
     * Specific accessor for finding {code JsonFormat.Value} for given type,
     * considering global default for leniency as well as per-type format
     * override (if any).
     *
     * @return Default format settings for type; never null.
     *
     * @since 2.10
     */
    public JsonFormat.Value findFormatDefaults(Class<?> type) {
        if (_overrides != null) {
            ConfigOverride override = _overrides.get(type);
            if (override != null) {
                JsonFormat.Value format = override.getFormat();
                if (format != null) {
                    if (!format.hasLenient()) {
                        return format.withLenient(_defaultLeniency);
                    }
                    return format;
                }
            }
        }
        if (_defaultLeniency == null) {
            return JsonFormat.Value.empty();
        }
        return JsonFormat.Value.forLeniency(_defaultLeniency);
    }

    /*
    /**********************************************************************
    /* Global defaults access
    /**********************************************************************
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

    /**
     * @since 2.10
     */
    public Boolean getDefaultLeniency() {
        return _defaultLeniency;
    }

    /**
     * @since 2.9
     */
    public VisibilityChecker<?> getDefaultVisibility() {
        return _visibilityChecker;
    }

    /**
     * @since 2.9
     */
    public void setDefaultInclusion(JsonInclude.Value v) {
        _defaultInclusion = v;
    }

    /**
     * @since 2.9
     */
    public void setDefaultSetterInfo(JsonSetter.Value v) {
        _defaultSetterInfo = v;
    }

    /**
     * @since 2.9
     */
    public void setDefaultMergeable(Boolean v) {
        _defaultMergeable = v;
    }

    /**
     * @since 2.10
     */
    public void setDefaultLeniency(Boolean v) {
        _defaultLeniency = v;
    }

    /**
     * @since 2.9
     */
    public void setDefaultVisibility(VisibilityChecker<?> v) {
        _visibilityChecker = v;
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected Map<Class<?>, MutableConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutableConfigOverride>();
    }
}

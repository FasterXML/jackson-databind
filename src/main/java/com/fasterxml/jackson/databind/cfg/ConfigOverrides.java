package com.fasterxml.jackson.databind.cfg;

import java.util.*;

/**
 * Container for individual {@link ConfigOverride} values.
 * 
 * @since 2.8
 */
public class ConfigOverrides
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected Map<Class<?>, MutableConfigOverride> _overrides;

    public ConfigOverrides() {
        _overrides = null;
    }

    protected ConfigOverrides(Map<Class<?>, MutableConfigOverride> overrides) {
        _overrides = overrides;
    }

    public ConfigOverrides copy()
    {
        if (_overrides == null) {
            return new ConfigOverrides();
        }
        Map<Class<?>, MutableConfigOverride> newOverrides = _newMap();
        for (Map.Entry<Class<?>, MutableConfigOverride> entry : _overrides.entrySet()) {
            newOverrides.put(entry.getKey(), entry.getValue().copy());
        }
        return new ConfigOverrides(newOverrides);
    }

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

    protected Map<Class<?>, MutableConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutableConfigOverride>();
    }
}

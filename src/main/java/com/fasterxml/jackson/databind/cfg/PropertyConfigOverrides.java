package com.fasterxml.jackson.databind.cfg;

import java.util.*;

/**
 * Container for individual {@link PropertyConfigOverride} values.
 * 
 * @since 2.8
 */
public class PropertyConfigOverrides
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected Map<Class<?>, MutablePropertyConfigOverride> _overrides;

    public PropertyConfigOverrides() {
        this(null);
    }

    protected PropertyConfigOverrides(Map<Class<?>, MutablePropertyConfigOverride> overrides) {
        _overrides = overrides;
    }

    public PropertyConfigOverrides copy() {
        if (_overrides == null) {
            return new PropertyConfigOverrides();
        }
        Map<Class<?>, MutablePropertyConfigOverride> newOverrides = _newMap();
        for (Map.Entry<Class<?>, MutablePropertyConfigOverride> entry : _overrides.entrySet()) {
            newOverrides.put(entry.getKey(), entry.getValue().copy());
        }
        return new PropertyConfigOverrides(newOverrides);
    }

    public PropertyConfigOverride findOverride(Class<?> type) {
        if (_overrides == null) {
            return null;
        }
        return _overrides.get(type);
    }

    public MutablePropertyConfigOverride findOrCreateOverride(Class<?> type) {
        if (_overrides == null) {
            _overrides = _newMap();
        }
        MutablePropertyConfigOverride override = _overrides.get(type);
        if (override == null) {
            override = new MutablePropertyConfigOverride();
            _overrides.put(type, override);
        }
        return override;
    }

    protected Map<Class<?>, MutablePropertyConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutablePropertyConfigOverride>();
    }
}

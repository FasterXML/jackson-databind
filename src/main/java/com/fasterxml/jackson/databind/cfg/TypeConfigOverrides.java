package com.fasterxml.jackson.databind.cfg;

import java.util.*;

/**
 * Container for individual {@link TypeConfigOverride} values.
 * 
 * @since 2.8
 */
public class TypeConfigOverrides
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected Map<Class<?>, MutableTypeConfigOverride> _overrides;

    public TypeConfigOverrides() {
        _overrides = null;
    }

    protected TypeConfigOverrides(Map<Class<?>, MutableTypeConfigOverride> overrides) {
        _overrides = overrides;
    }

    public TypeConfigOverrides copy()
    {
        if (_overrides == null) {
            return new TypeConfigOverrides();
        }
        Map<Class<?>, MutableTypeConfigOverride> newOverrides = _newMap();
        for (Map.Entry<Class<?>, MutableTypeConfigOverride> entry : _overrides.entrySet()) {
            newOverrides.put(entry.getKey(), entry.getValue().copy());
        }
        return new TypeConfigOverrides(newOverrides);
    }

    public TypeConfigOverride findOverride(Class<?> type) {
        if (_overrides == null) {
            return null;
        }
        return _overrides.get(type);
    }

    public MutableTypeConfigOverride findOrCreateOverride(Class<?> type) {
        if (_overrides == null) {
            _overrides = _newMap();
        }
        MutableTypeConfigOverride override = _overrides.get(type);
        if (override == null) {
            override = new MutableTypeConfigOverride();
            _overrides.put(type, override);
        }
        return override;
    }

    protected Map<Class<?>, MutableTypeConfigOverride> _newMap() {
        return new HashMap<Class<?>, MutableTypeConfigOverride>();
    }
}

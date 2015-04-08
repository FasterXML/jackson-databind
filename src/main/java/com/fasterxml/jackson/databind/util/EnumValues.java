package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

/**
 * Helper class used for storing String serializations of
 * enumerations.
 */
public final class EnumValues
{
    private final Class<Enum<?>> _enumClass;
    
    /**
     * Use a more optimized String value here, to possibly speed up
     * serialization.
     */
    private final EnumMap<?,SerializableString> _values;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private EnumValues(Class<Enum<?>> enumClass, Map<Enum<?>,SerializableString> v) {
        _enumClass = enumClass;
        _values = new EnumMap(v);
    }

    /**
     * NOTE: do NOT call this if configuration may change, and choice between toString()
     *   and name() might change dynamically.
     */
    public static EnumValues construct(SerializationConfig config, Class<Enum<?>> enumClass) {
        if (config.isEnabled(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)) {
            return constructFromToString(config, enumClass);
        }
        return constructFromName(config, enumClass);
    }

    public static EnumValues constructFromName(MapperConfig<?> config, Class<Enum<?>> enumClass)
    {
        // Enum types with per-instance sub-classes need special handling
        Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,SerializableString> map = new HashMap<Enum<?>,SerializableString>();
            for (Enum<?> en : values) {
                String value = config.getAnnotationIntrospector().findEnumValue(en);
                map.put(en, config.compileString(value));
            }
            return new EnumValues(enumClass, map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }

    public static EnumValues constructFromToString(MapperConfig<?> config, Class<Enum<?>> enumClass)
    {
        Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,SerializableString> map = new HashMap<Enum<?>,SerializableString>();
            for (Enum<?> en : values) {
                map.put(en, config.compileString(en.toString()));
            }
            return new EnumValues(enumClass, map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }

    public SerializableString serializedValueFor(Enum<?> key) { return _values.get(key); }
    public Collection<SerializableString> values() { return _values.values(); }

    /**
     * Method used for serialization and introspection by core Jackson code.
     */
    public EnumMap<?,SerializableString> internalMap() { return _values; }

    /**
     * @since 2.2
     */
    public Class<Enum<?>> getEnumClass() { return _enumClass; }
}

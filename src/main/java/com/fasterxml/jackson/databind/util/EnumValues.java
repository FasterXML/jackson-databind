package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.*;

/**
 * Helper class used for storing String serializations of
 * enumerations.
 */
public final class EnumValues
{
    /**
     * @since 2.2
     */
    private final Class<Enum<?>> _enumClass;
    
    /**
     * Since 1.7, we are storing values as SerializedStrings, to further
     * speed up serialization.
     */
    private final EnumMap<?,SerializedString> _values;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private EnumValues(Class<Enum<?>> enumClass, Map<Enum<?>,SerializedString> v) {
        _enumClass = enumClass;
        _values = new EnumMap(v);
    }

    public static EnumValues construct(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        return constructFromName(enumClass, intr);
    }

    public static EnumValues constructFromName(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        /* [JACKSON-214]: Enum types with per-instance sub-classes
         *   need special handling
         */
        Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,SerializedString> map = new HashMap<Enum<?>,SerializedString>();
            for (Enum<?> en : values) {
                String value = intr.findEnumValue(en);
                map.put(en, new SerializedString(value));
            }
            return new EnumValues(enumClass, map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }

    public static EnumValues constructFromToString(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,SerializedString> map = new HashMap<Enum<?>,SerializedString>();
            for (Enum<?> en : values) {
                map.put(en, new SerializedString(en.toString()));
            }
            return new EnumValues(enumClass, map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }

    public SerializedString serializedValueFor(Enum<?> key)
    {
        return _values.get(key);
    }
    
    public Collection<SerializedString> values() {
        return _values.values();
    }

    /**
     * Method used for serialization and introspection by core Jackson
     * code.
     * 
     * @since 2.1
     */
    public EnumMap<?,SerializedString> internalMap() {
        return _values;
    }

    /**
     * @since 2.2
     */
    public Class<Enum<?>> getEnumClass() {
        return _enumClass;
    }
    
}

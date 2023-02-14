package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

/**
 * Helper class used to resolve String values (either JSON Object field
 * names or regular String values) into Java Enum instances.
 */
public class EnumResolver implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final Class<Enum<?>> _enumClass;

    protected final Enum<?>[] _enums;

    protected final HashMap<String, Enum<?>> _enumsById;

    protected final Enum<?> _defaultValue;

    /**
     * Marker for case-insensitive handling
     *
     * @since 2.12
     */
    protected final boolean _isIgnoreCase;

    /**
     * Marker for case where value may come from {@code @JsonValue} annotated
     * accessor and is expected/likely to come from actual integral number
     * value (and not String).
     *<p>
     * Special case is needed since this specifically means that {@code Enum.index()}
     * should NOT be used or default to.
     *
     * @since 2.13
     */
    protected final boolean _isFromIntValue;

    /**
     * @since 2.12
     */
    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> map, Enum<?> defaultValue,
            boolean isIgnoreCase, boolean isFromIntValue)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
        _defaultValue = defaultValue;
        _isIgnoreCase = isIgnoreCase;
        _isFromIntValue = isFromIntValue;
    }

    /**
     * @deprecated Since 2.13
     */
    @Deprecated // since 2.13
    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> map, Enum<?> defaultValue,
            boolean isIgnoreCase) {
        this(enumClass, enums, map, defaultValue, isIgnoreCase, false);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value.
     *
     * @since 2.12
     */
    public static EnumResolver constructFor(DeserializationConfig config,
            Class<?> enumCls) {
        return _constructFor(enumCls, config.getAnnotationIntrospector(),
                config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    /**
     * @since 2.12
     */
    protected static EnumResolver _constructFor(Class<?> enumCls0,
            AnnotationIntrospector ai, boolean isIgnoreCase)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        String[] names = ai.findEnumValues(enumCls, enumConstants, new String[enumConstants.length]);
        final String[][] allAliases = new String[names.length][];
        ai.findEnumAliases(enumCls, enumConstants, allAliases);
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        for (int i = 0, len = enumConstants.length; i < len; ++i) {
            final Enum<?> enumValue = enumConstants[i];
            String name = names[i];
            if (name == null) {
                name = enumValue.name();
            }
            map.put(name, enumValue);
            String[] aliases = allAliases[i];
            if (aliases != null) {
                for (String alias : aliases) {
                    // Avoid overriding any primary names
                    map.putIfAbsent(alias, enumValue);
                }
            }
        }
        return new EnumResolver(enumCls, enumConstants, map,
                _enumDefault(ai, enumCls), isIgnoreCase,
                false);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     *
     * @since 2.12
     */
    public static EnumResolver constructUsingToString(DeserializationConfig config,
            Class<?> enumCls) {
        return _constructUsingToString(enumCls, config.getAnnotationIntrospector(),
                config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    /**
     * Factory method for constructing resolver that maps from index of Enum.values() into
     * Enum value
     *
     * @since 2.15
     */
    public static EnumResolver constructUsingIndex(DeserializationConfig config, Class<Enum<?>> enumCls) {
        return _constructUsingIndex(enumCls, config.getAnnotationIntrospector(),
                config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    private static EnumResolver _constructUsingIndex(Class<Enum<?>> enumCls0, AnnotationIntrospector ai, boolean isIgnoreCase) {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        HashMap<String, Enum<?>> map = new HashMap<>();

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> enumValue = enumConstants[i];
            map.put(String.valueOf(i), enumValue);
        }
        return new EnumResolver(enumCls, enumConstants, map,
            _enumDefault(ai, enumCls), isIgnoreCase, false);
    }

   /**
     * @since 2.12
     */
    protected static EnumResolver _constructUsingToString(Class<?> enumCls0,
            AnnotationIntrospector ai, boolean isIgnoreCase)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        final String[][] allAliases = new String[enumConstants.length][];
        if (ai != null) {
            ai.findEnumAliases(enumCls, enumConstants, allAliases);
        }

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> enumValue = enumConstants[i];
            map.put(enumValue.toString(), enumValue);
            String[] aliases = allAliases[i];
            if (aliases != null) {
                for (String alias : aliases) {
                    // Avoid overriding any primary names
                    map.putIfAbsent(alias, enumValue);
                }
            }
        }
        return new EnumResolver(enumCls, enumConstants, map,
                _enumDefault(ai, enumCls), isIgnoreCase, false);
    }

    /**
     * Method used when actual String serialization is indicated using @JsonValue
     * on a method in Enum class.
     *
     * @since 2.12
     */
    public static EnumResolver constructUsingMethod(DeserializationConfig config,
            Class<?> enumCls, AnnotatedMember accessor) {
        return _constructUsingMethod(enumCls, accessor, config.getAnnotationIntrospector(),
                config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    /**
     * @since 2.12
     */
    protected static EnumResolver _constructUsingMethod(Class<?> enumCls0,
            AnnotatedMember accessor, AnnotationIntrospector ai, boolean isIgnoreCase)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> en = enumConstants[i];
            try {
                Object o = accessor.getValue(en);
                if (o != null) {
                    map.put(o.toString(), en);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to access @JsonValue of Enum value "+en+": "+e.getMessage());
            }
        }
        return new EnumResolver(enumCls, enumConstants, map,
                _enumDefault(ai, enumCls), isIgnoreCase,
                // 26-Sep-2021, tatu: [databind#1850] Need to consider "from int" case
                _isIntType(accessor.getRawType())
        );
    }

    public CompactStringObjectMap constructLookup() {
        return CompactStringObjectMap.construct(_enumsById);
    }

    @SuppressWarnings("unchecked")
    protected static Class<Enum<?>> _enumClass(Class<?> enumCls0) {
        return (Class<Enum<?>>) enumCls0;
    }

    protected static Enum<?>[] _enumConstants(Class<?> enumCls) {
        final Enum<?>[] enumValues = _enumClass(enumCls).getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        return enumValues;
    }

    protected static Enum<?> _enumDefault(AnnotationIntrospector intr, Class<?> enumCls) {
        return (intr != null) ? intr.findDefaultEnumValue(_enumClass(enumCls)) : null;
    }

    protected static boolean _isIntType(Class<?> erasedType) {
        if (erasedType.isPrimitive()) {
            erasedType = ClassUtil.wrapperType(erasedType);
        }
        return (erasedType == Long.class)
                || (erasedType == Integer.class)
                || (erasedType == Short.class)
                || (erasedType == Byte.class)
                ;
    }

    /*
    /**********************************************************************
    /* Deprecated constructors, factory methods
    /**********************************************************************
     */

    /**
     * @deprecated Since 2.12 (remove from 2.13+ not part of public API)
     */
    @Deprecated // since 2.12
    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> map, Enum<?> defaultValue) {
        this(enumClass, enums, map, defaultValue, false, false);
    }

    /**
     * @deprecated Since 2.12
     */
    @Deprecated // since 2.12
    public static EnumResolver constructFor(Class<Enum<?>> enumCls, AnnotationIntrospector ai) {
        return _constructFor(enumCls, ai, false);
    }

    /**
     * @deprecated Since 2.12
     */
    @Deprecated // since 2.12
    public static EnumResolver constructUnsafe(Class<?> rawEnumCls, AnnotationIntrospector ai) {
        return _constructFor(rawEnumCls, ai, false);
    }

    /**
     * @deprecated Since 2.12
     */
    @Deprecated // since 2.12
    public static EnumResolver constructUsingToString(Class<Enum<?>> enumCls,
            AnnotationIntrospector ai) {
        return _constructUsingToString(enumCls, ai, false);
    }

    /**
     * @since 2.8
     * @deprecated Since 2.12
     */
    @Deprecated // since 2.12
    public static EnumResolver constructUnsafeUsingToString(Class<?> rawEnumCls,
            AnnotationIntrospector ai) {
        return _constructUsingToString(rawEnumCls, ai, false);
    }

    /**
     * @deprecated Since 2.8 (remove from 2.13 or later)
     */
    @Deprecated
    public static EnumResolver constructUsingToString(Class<Enum<?>> enumCls) {
        return _constructUsingToString(enumCls, null, false);
    }

    /**
     * @deprecated Since 2.12
     */
    @Deprecated
    public static EnumResolver constructUsingMethod(Class<Enum<?>> enumCls,
            AnnotatedMember accessor, AnnotationIntrospector ai) {
        return _constructUsingMethod(enumCls, accessor, ai, false);
    }

    /**
     * @since 2.9
     * @deprecated Since 2.12
     */
    @Deprecated
    public static EnumResolver constructUnsafeUsingMethod(Class<?> rawEnumCls,
            AnnotatedMember accessor, AnnotationIntrospector ai) {
        return _constructUsingMethod(rawEnumCls, accessor, ai, false);
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public Enum<?> findEnum(final String key) {
        Enum<?> en = _enumsById.get(key);
        if (en == null) {
            if (_isIgnoreCase) {
                return _findEnumCaseInsensitive(key);
            }
        }
        return en;
    }

    // @since 2.12
    protected Enum<?> _findEnumCaseInsensitive(final String key) {
        for (Map.Entry<String, Enum<?>> entry : _enumsById.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Enum<?> getEnum(int index) {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Enum<?> getDefaultValue(){
        return _defaultValue;
    }

    public Enum<?>[] getRawEnums() {
        return _enums;
    }

    public List<Enum<?>> getEnums() {
        ArrayList<Enum<?>> enums = new ArrayList<Enum<?>>(_enums.length);
        for (Enum<?> e : _enums) {
            enums.add(e);
        }
        return enums;
    }

    /**
     * @since 2.7.3
     */
    public Collection<String> getEnumIds() {
        return _enumsById.keySet();
    }

    public Class<Enum<?>> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }

    /**
     * Accessor for checking if we have a special case in which value to map
     * is from {@code @JsonValue} annotated accessor with integral type: this
     * matters for cases where incoming content value is of integral type
     * and should be mapped to specific value and NOT to {@code Enum.index()}.
     *
     * @since 2.13
     */
    public boolean isFromIntValue() {
        return _isFromIntValue;
    }
}


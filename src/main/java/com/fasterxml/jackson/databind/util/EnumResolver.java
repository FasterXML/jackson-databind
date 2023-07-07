package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.EnumNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;

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

    /*
    /**********************************************************************
    /* Constructors (non-deprecated)
    /**********************************************************************
     */

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

    /*
    /**********************************************************************
    /* Factory methods (non-deprecated)
    /**********************************************************************
     */

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value.
     *
     * @since 2.12
     * @deprecated Since 2.16 use {@link #constructFor(DeserializationConfig, AnnotatedClass)} instead
     */
    @Deprecated
    public static EnumResolver constructFor(DeserializationConfig config,
            Class<?> enumCls0)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
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
     * Factory method for constructing an {@link EnumResolver} based on the given {@link DeserializationConfig} and
     * {@link AnnotatedClass} of the enum to be resolved.
     *
     * @param config the deserialization configuration to use
     * @param annotatedClass the annotated class of the enum to be resolved
     * @return the constructed {@link EnumResolver}
     *
     * @since 2.16
     */
    public static EnumResolver constructFor(DeserializationConfig config, AnnotatedClass annotatedClass)
    {
        // prepare data
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        final Class<?> enumCls0 = annotatedClass.getRawType();
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);

        // introspect
        String[] names = ai.findEnumValues(config, annotatedClass,
                enumConstants, new String[enumConstants.length]);
        final String[][] allAliases = new String[names.length][];
        ai.findEnumAliases(config, annotatedClass, enumConstants, allAliases);

        // finally, build
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
            _enumDefault(ai, enumCls), isIgnoreCase, false);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     *
     * @since 2.12
     * @deprecated Since 2.16 use {@link #constructUsingToString(DeserializationConfig, AnnotatedClass)} instead
     */
    @Deprecated
    public static EnumResolver constructUsingToString(DeserializationConfig config,
            Class<?> enumCls0)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
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
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     *
     * @since 2.16
     */
    public static EnumResolver constructUsingToString(DeserializationConfig config, AnnotatedClass annotatedClass) {
        // prepare data
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        final Class<?> enumCls0 = annotatedClass.getRawType();
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);

        // introspect
        final String[][] allAliases = new String[enumConstants.length][];
        if (ai != null) {
            ai.findEnumAliases(config, annotatedClass, enumConstants, allAliases);
        }
        
        // finally, build
        // from last to first, so that in case of duplicate values, first wins
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
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
     * Factory method for constructing resolver that maps from index of Enum.values() into
     * Enum value
     *
     * @since 2.15
     */
    public static EnumResolver constructUsingIndex(DeserializationConfig config,
            Class<Enum<?>> enumCls0)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
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
     * Factory method for constructing resolver that maps the name of enums converted to external property
     * names into Enum value using an implementation of {@link EnumNamingStrategy}.
     *
     * The output {@link EnumResolver} should contain values that are symmetric to
     * {@link EnumValues#constructUsingEnumNamingStrategy(MapperConfig, Class, EnumNamingStrategy)}.
     * @since 2.15
     */
    public static EnumResolver constructUsingEnumNamingStrategy(DeserializationConfig config,
            Class<?> enumCls, EnumNamingStrategy enumNamingStrategy) {
        return _constructUsingEnumNamingStrategy(config, enumCls, enumNamingStrategy);
    }

    /**
     * Internal method for
     * {@link EnumResolver#constructUsingEnumNamingStrategy(DeserializationConfig, Class, EnumNamingStrategy)}
     * 
     * @since 2.15
     */
    private static EnumResolver _constructUsingEnumNamingStrategy(
        DeserializationConfig config, Class<?> enumCls0, EnumNamingStrategy enumNamingStrategy)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        HashMap<String, Enum<?>> map = new HashMap<>();

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> anEnum = enumConstants[i];
            String translatedExternalValue = enumNamingStrategy.convertEnumToExternalName(anEnum.name());
            map.put(translatedExternalValue, anEnum);
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
            Class<?> enumCls0, AnnotatedMember accessor)
    {
        final AnnotationIntrospector ai = config.getAnnotationIntrospector();
        final boolean isIgnoreCase = config.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
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
     * @deprecated Since 2.13 -- remove from 2.16
     */
    @Deprecated // since 2.13
    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> map, Enum<?> defaultValue,
            boolean isIgnoreCase) {
        this(enumClass, enums, map, defaultValue, isIgnoreCase, false);
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


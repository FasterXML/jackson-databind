package com.fasterxml.jackson.databind.util;

import java.util.*;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;

import static com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

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

    protected final boolean _isIgnoreCase;

    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> enumsById, Enum<?> defaultValue, boolean isIgnoreCase)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = enumsById;
        _defaultValue = defaultValue;
        _isIgnoreCase = isIgnoreCase;
    }

    protected static EnumResolver _construct(DeserializationConfig config,
            Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> enumsById)
    {
        return new EnumResolver(enumClass, enums, enumsById,
                config.getAnnotationIntrospector().findDefaultEnumValue(config, enumClass),
                config.isEnabled(ACCEPT_CASE_INSENSITIVE_ENUMS));
    }

    /**
     * Factory method for constructing resolver that maps from Enum.name() into
     * Enum value
     */
    public static EnumResolver constructFor(DeserializationConfig config, Class<?> enumCls0)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls);
        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        final String[] names = intr.findEnumValues(config,
                enumCls, enumConstants, new String[enumConstants.length]);
        final String[][] allAliases = new String[names.length][];
        final HashMap<String, Enum<?>> byId = new HashMap<String, Enum<?>>();

        intr.findEnumAliases(config, enumCls, enumConstants, allAliases);

        for (int i = 0, len = enumConstants.length; i < len; ++i) {
            final Enum<?> enumValue = enumConstants[i];
            String name = names[i];
            if (name == null) {
                name = enumValue.name();
            }
            byId.put(name, enumValue);
            String[] aliases = allAliases[i];
            if (aliases != null) {
                for (String alias : aliases) {
                    // avoid accidental override of primary name (in case of conflicting annotations)
                    byId.putIfAbsent(alias, enumValue);
                }
            }
        }
        return _construct(config, enumCls, enumConstants, byId);
    }

    /**
     * Factory method for constructing resolver that maps from Enum.toString() into
     * Enum value
     */
    public static EnumResolver constructUsingToString(DeserializationConfig config,
            Class<?> enumCls0)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls);
        final HashMap<String, Enum<?>> byId = new HashMap<String, Enum<?>>();
        final String[][] allAliases = new String[enumConstants.length][];

        final AnnotationIntrospector intr = config.getAnnotationIntrospector();
        if (intr != null) {
            intr.findEnumAliases(config, enumCls, enumConstants, allAliases);
        }

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> enumValue = enumConstants[i];
            byId.put(enumValue.toString(), enumValue);
            String[] aliases = allAliases[i];
            if (aliases != null) {
                for (String alias : aliases) {
                    // avoid accidental override of primary name (in case of conflicting annotations)
                    byId.putIfAbsent(alias, enumValue);
                }
            }
        }
        return _construct(config, enumCls, enumConstants, byId);
    }

    public static EnumResolver constructUsingMethod(DeserializationConfig config,
            Class<?> enumCls0, AnnotatedMember accessor)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls);
        final HashMap<String, Enum<?>> byId = new HashMap<String, Enum<?>>();

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> en = enumConstants[i];
            try {
                Object o = accessor.getValue(en);
                if (o != null) {
                    byId.put(o.toString(), en);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to access @JsonValue of Enum value "+en+": "+e.getMessage());
            }
        }
        return _construct(config, enumCls, enumConstants, byId);
    }

    @SuppressWarnings("unchecked")
    protected static Class<Enum<?>> _enumClass(Class<?> cls) {
        return (Class<Enum<?>>) cls;
    }

    protected static Enum<?>[] _enumConstants(Class<Enum<?>> enumCls) {
        final Enum<?>[] ecs = enumCls.getEnumConstants();
        if (ecs == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        return ecs;
    }
    
    public CompactStringObjectMap constructLookup() {
        return CompactStringObjectMap.construct(_enumsById);
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */
    
    public Enum<?> findEnum(String key) {
        Enum<?> en = _enumsById.get(key);
        if (en == null) {
            if (_isIgnoreCase) {
                return _findEnumIgnoreCase(key);
            }
        }
        return en;
    }

    private final Enum<?> _findEnumIgnoreCase(final String key) {
        return _enumsById.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(e -> e.getValue())
                .findFirst()
                .orElseGet(null);
    }

    public Enum<?> getEnum(int index) {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Enum<?> getDefaultValue() {
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

    public Collection<String> getEnumIds() {
        return _enumsById.keySet();
    }
    
    public Class<Enum<?>> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}


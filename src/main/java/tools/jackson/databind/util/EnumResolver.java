package tools.jackson.databind.util;

import static tools.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS;

import java.util.*;

import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.introspect.AnnotatedMember;

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
     */
    protected final boolean _isIgnoreCase;

    /**
     * Marker for case where value may come from {@code @JsonValue} annotated
     * accessor and is expected/likely to come from actual integral number
     * value (and not String).
     *<p>
     * Special case is needed since this specifically means that {@code Enum.index()}
     * should NOT be used or default to.
     */
    protected final boolean _isFromIntValue;

    protected EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> enumsById, Enum<?> defaultValue,
            boolean isIgnoreCase, boolean isFromIntValue)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = enumsById;
        _defaultValue = defaultValue;
        _isIgnoreCase = isIgnoreCase;
        _isFromIntValue = isFromIntValue;
    }

    protected static EnumResolver _construct(DeserializationConfig config,
            Class<Enum<?>> enumClass, Enum<?>[] enums,
            HashMap<String, Enum<?>> enumsById,
            boolean isFromIntValue)
    {
        return new EnumResolver(enumClass, enums, enumsById,
                config.getAnnotationIntrospector().findDefaultEnumValue(config, enumClass),
                config.isEnabled(ACCEPT_CASE_INSENSITIVE_ENUMS),
                isFromIntValue);
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
        return _construct(config, enumCls, enumConstants, byId, false);
    }

    /**
     * Factory method for constructing resolver that maps from index of Enum.values() into
     * Enum value
     */
    public static EnumResolver constructUsingIndex(DeserializationConfig config,
            Class<Enum<?>> enumCls0)
    {
        final Class<Enum<?>> enumCls = _enumClass(enumCls0);
        final Enum<?>[] enumConstants = _enumConstants(enumCls0);
        HashMap<String, Enum<?>> map = new HashMap<>();

        // from last to first, so that in case of duplicate values, first wins
        for (int i = enumConstants.length; --i >= 0; ) {
            Enum<?> enumValue = enumConstants[i];
            map.put(String.valueOf(i), enumValue);
        }
        return _construct(config, enumCls, enumConstants, map, false);
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
        return _construct(config, enumCls, enumConstants, byId, false);
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
        return _construct(config, enumCls, enumConstants, byId,
                // 26-Sep-2021, tatu: [databind#1850] Need to consider "from int" case
                _isIntType(accessor.getRawType()));
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

    /**
     * Accessor for checking if we have a special case in which value to map
     * is from {@code @JsonValue} annotated accessor with integral type: this
     * matters for cases where incoming content value is of integral type
     * and should be mapped to specific value and NOT to {@code Enum.index()}.
     */
    public boolean isFromIntValue() {
        return _isFromIntValue;
    }
}

package tools.jackson.databind.util;

import java.util.*;

import tools.jackson.core.SerializableString;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;

/**
 * Helper class used for storing String serializations of {@code Enum}s,
 * to match to external representations.
 *
 * @deprecated Since 3.1 should no longer be used (replaced by {@link EnumValuesToWrite}).
 */
@Deprecated
public final class EnumValues
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1;

    private final Class<Enum<?>> _enumClass;

    private final Enum<?>[] _values;
    private final SerializableString[] _textual;

    private transient EnumMap<?,SerializableString> _asMap;

    private EnumValues(Class<Enum<?>> enumClass, SerializableString[] textual)
    {
        _enumClass = enumClass;
        _values = enumClass.getEnumConstants();
        _textual = textual;
    }

    public static EnumValues construct(SerializationConfig config, AnnotatedClass enumClass) {
        if (config.isEnabled(EnumFeature.WRITE_ENUMS_USING_TO_STRING)) {
            return constructFromToString(config, enumClass);
        }
        return constructFromName(config, enumClass);
    }

    public static EnumValues constructFromName(MapperConfig<?> config,
            AnnotatedClass annotatedClass) 
    {
        final EnumDefinition def = EnumDefinition.construct(config, annotatedClass);
        final Class<Enum<?>> enumCls = def.enumClass();
        final Enum<?>[] enumConstants = def.enumConstants();

        List<String> explicitNames = def.explicitNames();
        SerializableString[] textual = new SerializableString[enumConstants.length];
        final boolean useLowerCase = config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
        for (int i = 0, len = enumConstants.length; i < len; ++i) {
            Enum<?> enumValue = enumConstants[i];
            String name = _findNameToUse(explicitNames.get(i), enumValue.name(), useLowerCase);
            textual[enumValue.ordinal()] = config.compileString(name);
        }
        return construct(enumCls, textual);
    }

    public static EnumValues constructFromToString(MapperConfig<?> config,
            AnnotatedClass annotatedClass)
    {
        final EnumDefinition def = EnumDefinition.construct(config, annotatedClass);
        final Class<Enum<?>> enumCls = def.enumClass();
        final Enum<?>[] enumConstants = def.enumConstants();

        List<String> explicitNames = def.explicitNames();
        SerializableString[] textual = new SerializableString[enumConstants.length];
        final boolean useLowerCase = config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
        for (int i = 0; i < enumConstants.length; i++) {
            String enumToString = enumConstants[i].toString();
            // 01-Feb-2024, tatu: [databind#4355] Nulls not great but... let's
            //   coerce into "" for backwards compatibility
            enumToString = (enumToString == null) ? "" : enumToString;
            String name = _findNameToUse(explicitNames.get(i), enumToString, useLowerCase);
            textual[i] = config.compileString(name);
        }
        return construct(enumCls, textual);
    }

    /**
     * Returns String serializations of Enum name using an instance of {@link EnumNamingStrategy}.
     * <p>
     * The output {@link EnumValues} should contain values that are symmetric to
     * {@link EnumResolver#constructUsingEnumNamingStrategy(DeserializationConfig, AnnotatedClass, EnumNamingStrategy)}.
     */
    public static EnumValues constructUsingEnumNamingStrategy(MapperConfig<?> config,
            AnnotatedClass annotatedClass,
            EnumNamingStrategy namingStrategy)
    {
        final EnumDefinition def = EnumDefinition.construct(config, annotatedClass);
        final Class<Enum<?>> enumCls = def.enumClass();
        final Enum<?>[] enumConstants = def.enumConstants();

        List<String> explicitNames = def.explicitNames();
        SerializableString[] textual = new SerializableString[enumConstants.length];
        final boolean useLowerCase = config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE);
        for (int i = 0, len = enumConstants.length; i < len; i++) {
            Enum<?> enumValue = enumConstants[i];
            String name = _findNameToUse(explicitNames.get(i), namingStrategy.convertEnumToExternalName(config,
                    annotatedClass, enumValue.name()), useLowerCase);
            textual[i] = config.compileString(name);
        }
        return construct(enumCls, textual);
    }

    public static EnumValues construct(MapperConfig<?> config, Class<Enum<?>> enumClass,
            List<String> externalValues) {
        final int len = externalValues.size();
        SerializableString[] textual = new SerializableString[len];
        for (int i = 0; i < len; ++i) {
            textual[i] = config.compileString(externalValues.get(i));
        }
        return construct(enumClass, textual);
    }

    public static EnumValues construct(Class<Enum<?>> enumClass,
            SerializableString[] externalValues) {
        return new EnumValues(enumClass, externalValues);
    }

    /* 
    /**********************************************************************
    /* Internal Helpers
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    protected static Class<Enum<?>> _enumClass(Class<?> enumCls0) {
        return (Class<Enum<?>>) enumCls0;
    }

    /**
     * Helper method <b>slightly</b> different from {@link EnumResolver#_enumConstants(Class)},
     * with same method name to keep calling methods more consistent.
     */
    protected static Enum<?>[] _enumConstants(Class<?> enumCls) {
        final Enum<?>[] enumValues = ClassUtil.findEnumType(enumCls).getEnumConstants();
        if (enumValues == null) {
            throw new IllegalArgumentException("No enum constants for class "+enumCls.getName());
        }
        return enumValues;
    }

    protected static String _findNameToUse(String explicitName, String otherName, boolean toLowerCase) {
        // If explicitly named, like @JsonProperty-annotated, then use it
        if (explicitName != null) {
            return explicitName;
        }
        // [databind#4788] Since 2.18.2 : EnumFeature.WRITE_ENUMS_TO_LOWERCASE should not
        //                 override @JsonProperty values
        if (toLowerCase) {
            return otherName.toLowerCase();
        }
        return otherName;
    }

    /*
    /**********************************************************************
    /* Public API
    /**********************************************************************
     */

    public SerializableString serializedValueFor(Enum<?> key) {
        return _textual[key.ordinal()];
    }

    public Collection<SerializableString> values() {
        return Arrays.asList(_textual);
    }

    /**
     * Convenience accessor for getting raw Enum instances.
     */
    public List<Enum<?>> enums() {
        return Arrays.asList(_values);
    }

    /**
     * Method used for serialization and introspection by core Jackson code.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public EnumMap<?,SerializableString> internalMap() {
        EnumMap<?,SerializableString> result = _asMap;
        if (result == null) {
            // Alas, need to create it in a round-about way, due to typing constraints...
            Map<Enum<?>,SerializableString> map = new LinkedHashMap<>();
            for (Enum<?> en : _values) {
                map.put(en, _textual[en.ordinal()]);
            }
            _asMap = result = new EnumMap(map);
        }
        return result;
    }

    public Class<Enum<?>> getEnumClass() { return _enumClass; }
}

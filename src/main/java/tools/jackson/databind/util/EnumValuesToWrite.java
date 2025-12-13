package tools.jackson.databind.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import tools.jackson.core.SerializableString;
import tools.jackson.databind.EnumNamingStrategy;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;

/**
 * @since 3.0.3
 */
public class EnumValuesToWrite
{
    private final AnnotatedClass _annotatedClass;
    private final EnumNamingStrategy _enumNamingStrategy;
    private final Enum<?>[] _enumConstants;
    private final SerializableString[] _explicitNames;

    private volatile SerializableString[] _enumNames;
    private volatile SerializableString[] _enumNamesLC;
    private volatile SerializableString[] _enumToStrings;
    private volatile SerializableString[] _enumToStringsLC;

    private EnumValuesToWrite(AnnotatedClass annotatedClass,
            EnumNamingStrategy enumNamingStrategy,
            Enum<?>[] enumConstants, SerializableString[] explicitNames)
    {
        _annotatedClass = annotatedClass;
        _enumNamingStrategy = enumNamingStrategy;
        _enumConstants = enumConstants;
        _explicitNames = explicitNames;
    }

    public static EnumValuesToWrite construct(MapperConfig<?> config,
            AnnotatedClass annotatedClass,
            EnumNamingStrategy enumNamingStrategy,
            Enum<?>[] enumConstants, String[] explicitNames0)
    {
        final int len = explicitNames0.length;
        SerializableString[] explicitNames = new SerializableString[len];
        for (int i = 0; i < len; ++i) {
            explicitNames[i] = config.compileString(explicitNames0[i]);
        }
        return new EnumValuesToWrite(annotatedClass,
                enumNamingStrategy, enumConstants, explicitNames);
    }

    @SuppressWarnings("unchecked")
    public Class<Enum<?>> enumClass() {
        Class<?> cls = _annotatedClass.getRawType();
        return (Class<Enum<?>>) cls;
    }

    public List<Enum<?>> enums() {
        return Arrays.asList(_enumConstants);
    }

    public SerializableString enumValueFromName(MapperConfig<?> config, Enum<?> en) {
        return allEnumValuesFromName(config)[en.ordinal()];
    }

    public SerializableString[] allEnumValuesFromName(MapperConfig<?> config) {
        SerializableString[] strs;
        if (config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)) {
            if ((strs = _enumNamesLC) == null) {
                _enumNamesLC = strs = _fetch(config,
                        e -> _nameWithStrategy(config, e),
                        true);
            }
        } else {
            if ((strs = _enumNames) == null) {
                _enumNames = strs = _fetch(config,
                        e -> _nameWithStrategy(config, e),
                        false);
            }
        }
        return strs;
    }

    public SerializableString enumValueFromToString(MapperConfig<?> config, Enum<?> en) {
        return allEnumValuesFromToString(config)[en.ordinal()];
    }

    public SerializableString[] allEnumValuesFromToString(MapperConfig<?> config) {
        SerializableString[] strs;
        if (config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)) {
            if ((strs = _enumToStringsLC) == null) {
                _enumToStringsLC = strs = _fetch(config,
                        e -> _toStringWithStrategy(config, e),
                        true);
            }
        } else {
            if ((strs = _enumToStrings) == null) {
                _enumToStrings = strs = _fetch(config,
                        e -> _toStringWithStrategy(config, e),
                        false);
            }
        }
        return strs;
    }

    private String _nameWithStrategy(MapperConfig<?> config, Enum<?> en) {
        String str = en.name();
        if (_enumNamingStrategy != null) {
            str = _enumNamingStrategy.convertEnumToExternalName(config, _annotatedClass, str);
        }
        return str;
    }

    private String _toStringWithStrategy(MapperConfig<?> config, Enum<?> en) {
        String str = en.toString();
        if (_enumNamingStrategy != null) {
            str = _enumNamingStrategy.convertEnumToExternalName(config, _annotatedClass, str);
        }
        return str;
    }

    private SerializableString[] _fetch(MapperConfig<?> config,
            Function<Enum<?>,String> accessor, boolean lowerCase) {
        final int len = _enumConstants.length;
        SerializableString[] serStrs = new SerializableString[_enumConstants.length];
        for (int i = 0; i < len; ++i) {
            SerializableString ser = _explicitNames[i];
            if (ser == null) {
                String str = accessor.apply(_enumConstants[i]);
                if (lowerCase) {
                    str = str.toLowerCase();
                }
                ser = config.compileString(str);
            }
            serStrs[i] = ser;
        }
        return serStrs;
    }
}

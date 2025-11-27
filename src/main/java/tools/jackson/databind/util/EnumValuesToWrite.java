package tools.jackson.databind.util;

import java.util.function.Function;

import tools.jackson.core.SerializableString;
import tools.jackson.databind.EnumNamingStrategy;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.MapperConfig;

/**
 * @since 3.0.3
 */
public class EnumValuesToWrite
{
    private final EnumNamingStrategy _enumNamingStrategy;
    private final Enum<?>[] _enumConstants;
    private final SerializableString[] _explicitNames;

    private volatile SerializableString[] _enumNames;
    private volatile SerializableString[] _enumNamesLC;
    private volatile SerializableString[] _enumToStrings;
    private volatile SerializableString[] _enumToStringsLC;

    private EnumValuesToWrite(EnumNamingStrategy enumNamingStrategy,
            Enum<?>[] enumConstants, SerializableString[] explicitNames)
    {
        _enumNamingStrategy = enumNamingStrategy;
        _enumConstants = enumConstants;
        _explicitNames = explicitNames;
    }

    public static EnumValuesToWrite construct(MapperConfig<?> config,
            EnumNamingStrategy enumNamingStrategy,
            Enum<?>[] enumConstants, String[] explicitNames0)
    {
        final int len = explicitNames0.length;
        SerializableString[] explicitNames = new SerializableString[len];
        for (int i = 0; i < len; ++i) {
            explicitNames[i] = config.compileString(explicitNames0[i]);
        }
        return new EnumValuesToWrite(enumNamingStrategy, enumConstants, explicitNames);
    }

    public SerializableString fromName(MapperConfig<?> config, Enum<?> en) {
        SerializableString[] strs;
        if (config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)) {
            if ((strs = _enumNamesLC) == null) {
                _enumNamesLC = strs = _fetch(config, Enum::name, true);
            }
        } else {
            if ((strs = _enumNames) == null) {
                _enumNames = strs = _fetch(config, Enum::name, false);
            }
        }
        return strs[en.ordinal()];
    }

    public SerializableString fromToString(MapperConfig<?> config, Enum<?> en) {
        SerializableString[] strs;
        if (config.isEnabled(EnumFeature.WRITE_ENUMS_TO_LOWERCASE)) {
            if ((strs = _enumToStringsLC) == null) {
                _enumToStringsLC = strs = _fetch(config, Enum::toString, true);
            }
        } else {
            if ((strs = _enumToStrings) == null) {
                _enumToStrings = strs = _fetch(config, Enum::toString, false);
            }
        }
        return strs[en.ordinal()];
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

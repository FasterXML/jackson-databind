package com.fasterxml.jackson.databind.cfg;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * @since 2.12
 */
public class CoercionConfigs
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    private final static int TARGET_TYPE_COUNT = LogicalType.values().length;

    /**
     * Global default for cases not explicitly covered
     */
    protected CoercionAction _defaultAction;

    /**
     * Default coercion definitions used if no overrides found
     * by logical or physical type.
     */
    protected final MutableCoercionConfig _defaultCoercions;

    /**
     * Coercion definitions by logical type ({@link LogicalType})
     */
    protected MutableCoercionConfig[] _perTypeCoercions;

    /**
     * Coercion definitions by physical type (Class).
     */
    protected Map<Class<?>, MutableCoercionConfig> _perClassCoercions;

    /*
    /**********************************************************************
    /* Life cycle
    /**********************************************************************
     */

    public CoercionConfigs() {
        this(CoercionAction.TryConvert, new MutableCoercionConfig(),
                null, null);
    }

    protected CoercionConfigs(CoercionAction defaultAction,
            MutableCoercionConfig defaultCoercions,
            MutableCoercionConfig[] perTypeCoercions,
            Map<Class<?>, MutableCoercionConfig> perClassCoercions)
    {
        _defaultCoercions = defaultCoercions;
        _defaultAction = defaultAction;
        _perTypeCoercions = perTypeCoercions;
        _perClassCoercions = perClassCoercions;
    }

    /**
     * Method called to create a non-shared copy of configuration settings,
     * to be used by another {@link com.fasterxml.jackson.databind.ObjectMapper}
     * instance.
     *
     * @return A non-shared copy of configuration settings
     */
    public CoercionConfigs copy()
    {
        MutableCoercionConfig[] newPerType;
        if (_perTypeCoercions == null) {
            newPerType = null;
        } else {
            final int size = _perTypeCoercions.length;
            newPerType = new MutableCoercionConfig[size];
            for (int i = 0; i < size; ++i) {
                newPerType[i] = _copy(_perTypeCoercions[i]);
            }
        }
        Map<Class<?>, MutableCoercionConfig> newPerClass;
        if (_perClassCoercions == null) {
            newPerClass = null;
        } else {
            newPerClass = new HashMap<>();
            for (Map.Entry<Class<?>, MutableCoercionConfig> entry : _perClassCoercions.entrySet()) {
                newPerClass.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return new CoercionConfigs(_defaultAction, _defaultCoercions.copy(),
                newPerType, newPerClass);
    }

    private static MutableCoercionConfig _copy(MutableCoercionConfig src) {
        if (src == null) {
            return null;
        }
        return src.copy();
    }

    /*
    /**********************************************************************
    /* Mutators: global defaults
    /**********************************************************************
     */

    public MutableCoercionConfig defaultCoercions() {
        return _defaultCoercions;
    }

    /*
    /**********************************************************************
    /* Mutators: per type
    /**********************************************************************
     */

    public MutableCoercionConfig findOrCreateCoercion(LogicalType type) {
        if (_perTypeCoercions == null) {
            _perTypeCoercions = new MutableCoercionConfig[TARGET_TYPE_COUNT];
        }
        MutableCoercionConfig config = _perTypeCoercions[type.ordinal()];
        if (config == null) {
            _perTypeCoercions[type.ordinal()] = config = new MutableCoercionConfig();
        }
        return config;
    }

    public MutableCoercionConfig findOrCreateCoercion(Class<?> type) {
        if (_perClassCoercions == null) {
            _perClassCoercions = new HashMap<>();
        }
        MutableCoercionConfig config = _perClassCoercions.get(type);
        if (config == null) {
            config = new MutableCoercionConfig();
            _perClassCoercions.put(type, config);
        }
        return config;
    }

    /*
    /**********************************************************************
    /* Access
    /**********************************************************************
     */

    /**
     * General-purpose accessor for finding what to do when specified coercion
     * from shape that is now always allowed to be coerced from is requested.
     *
     * @param config Currently active deserialization configuration
     * @param targetType Logical target type of coercion
     * @param targetClass Physical target type of coercion
     * @param inputShape Input shape to coerce from
     *
     * @return CoercionAction configured for specified coercion
     *
     * @since 2.12
     */
    public CoercionAction findCoercion(DeserializationConfig config,
            LogicalType targetType,
            Class<?> targetClass, CoercionInputShape inputShape)
    {
        // First, see if there is exact match for physical type
        if ((_perClassCoercions != null) && (targetClass != null)) {
            MutableCoercionConfig cc = _perClassCoercions.get(targetClass);
            if (cc != null) {
                CoercionAction act = cc.findAction(inputShape);
                if (act != null) {
                    return act;
                }
            }
        }

        // If not, maybe by logical type
        if ((_perTypeCoercions != null) && (targetType != null)) {
            MutableCoercionConfig cc = _perTypeCoercions[targetType.ordinal()];
            if (cc != null) {
                CoercionAction act = cc.findAction(inputShape);
                if (act != null) {
                    return act;
                }
            }
        }

        // Barring that, default coercion for input shape?
        CoercionAction act = _defaultCoercions.findAction(inputShape);
        if (act != null) {
            return act;
        }

        // Otherwise there are some legacy features that can provide answer
        switch (inputShape) {
        case EmptyArray:
            // Default for setting is false
            return config.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT) ?
                    CoercionAction.AsNull : CoercionAction.Fail;
        case Float:
            if (targetType == LogicalType.Integer) {
                // Default for setting in 2.x is true
                return config.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT) ?
                        CoercionAction.TryConvert : CoercionAction.Fail;
            }
            break;
        case Integer:
            if (targetType == LogicalType.Enum) {
                if (config.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
                    return CoercionAction.Fail;
                }
            }
            break;
        default:
        }

        // classic scalars are numbers, booleans; but date/time also considered
        // scalar for this particular purpose
        final boolean baseScalar = _isScalarType(targetType);

        if (baseScalar
                // Default for setting in 2.x is true
                && !config.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // 12-Oct-2022, carterkozak: As per [databind#3624]: Coercion from integer-shaped
                // data into a floating point type is not banned by the
                // ALLOW_COERCION_OF_SCALARS feature because '1' is a valid JSON representation of
                // '1.0' in a way that other types of coercion do not satisfy.
                && (targetType != LogicalType.Float || inputShape != CoercionInputShape.Integer)) {
            return CoercionAction.Fail;
        }

        if (inputShape == CoercionInputShape.EmptyString) {
            // Since coercion of scalar must be enabled (see check above), allow empty-string
            // coercions by default even without this setting
            if (baseScalar
                    // Default for setting is false
                    || config.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                return CoercionAction.AsNull;
            }
            // 09-Jun-2020, tatu: Seems necessary to support backwards-compatibility with
            //     2.11, wrt "FromStringDeserializer" supported types
            if (targetType == LogicalType.OtherScalar) {
                return CoercionAction.TryConvert;
            }
            // But block from allowing structured types like POJOs, Maps etc
            return CoercionAction.Fail;
        }

        // and all else failing, return default
        return _defaultAction;
    }

    /**
     * More specialized accessor called in case of input being a blank
     * String (one consisting of only white space characters with length of at least one).
     * Will basically first determine if "blank as empty" is allowed: if not,
     * returns {@code actionIfBlankNotAllowed}, otherwise returns action for
     * {@link CoercionInputShape#EmptyString}.
     *
     * @param config Currently active deserialization configuration
     * @param targetType Logical target type of coercion
     * @param targetClass Physical target type of coercion
     * @param actionIfBlankNotAllowed Return value to use in case "blanks as empty"
     *    is not allowed
     *
     * @return CoercionAction configured for specified coercion from blank string
     */
    public CoercionAction findCoercionFromBlankString(DeserializationConfig config,
            LogicalType targetType,
            Class<?> targetClass,
            CoercionAction actionIfBlankNotAllowed)
    {
        Boolean acceptBlankAsEmpty = null;
        CoercionAction action = null;

        // First, see if there is exact match for physical type
        if ((_perClassCoercions != null) && (targetClass != null)) {
            MutableCoercionConfig cc = _perClassCoercions.get(targetClass);
            if (cc != null) {
                acceptBlankAsEmpty = cc.getAcceptBlankAsEmpty();
                action = cc.findAction(CoercionInputShape.EmptyString);
            }
        }

        // If not, maybe by logical type
        if ((_perTypeCoercions != null) && (targetType != null)) {
            MutableCoercionConfig cc = _perTypeCoercions[targetType.ordinal()];
            if (cc != null) {
                if (acceptBlankAsEmpty == null) {
                    acceptBlankAsEmpty = cc.getAcceptBlankAsEmpty();
                }
                if (action == null) {
                    action = cc.findAction(CoercionInputShape.EmptyString);
                }
            }
        }

        // Barring that, default coercion for input shape?
        if (acceptBlankAsEmpty == null) {
            acceptBlankAsEmpty = _defaultCoercions.getAcceptBlankAsEmpty();
        }
        if (action == null) {
            action = _defaultCoercions.findAction(CoercionInputShape.EmptyString);
        }

        // First: if using blank as empty is no-go, return what caller specified
        if (Boolean.FALSE.equals(acceptBlankAsEmpty)) {
            return actionIfBlankNotAllowed;
        }
        // Otherwise, if action found, return that
        if (action != null) {
            return action;
        }

        // 23-Sep-2021, tatu: [databind#3234] Should default to "allow" for Scalar types
        //    for backwards compatibility
        if (_isScalarType(targetType)) {
            return CoercionAction.AsNull;
        }

        // If not, one specific legacy setting to consider...
        if (config.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
            return CoercionAction.AsNull;
        }

        // But finally consider ultimate default to be "false" and so:
        return actionIfBlankNotAllowed;
    }

    protected boolean _isScalarType(LogicalType targetType) {
        return (targetType == LogicalType.Float)
                || (targetType == LogicalType.Integer)
                || (targetType == LogicalType.Boolean)
                || (targetType == LogicalType.DateTime);
    }
}

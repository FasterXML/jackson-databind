package tools.jackson.databind.deser.jdk;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.SettableBeanProperty;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.ClassUtil;
import tools.jackson.databind.util.CompactStringObjectMap;
import tools.jackson.databind.util.EnumResolver;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
@JacksonStdImpl
public class EnumDeserializer
    extends StdScalarDeserializer<Object>
{
    protected Object[] _enumsByIndex;

    private final Enum<?> _enumDefaultValue;

    protected final CompactStringObjectMap _lookupByName;

    /**
     * Alternatively, we may need a different lookup object if "use toString"
     * is defined.
     */
    protected CompactStringObjectMap _lookupByToString;

    protected final Boolean _caseInsensitive;

    /**
     * Marker flag for cases where we expect actual integral value for Enum,
     * based on {@code @JsonValue} (and equivalent) annotated accessor.
     */
    protected final boolean _isFromIntValue;

    public EnumDeserializer(EnumResolver byNameResolver, Boolean caseInsensitive)
    {
        super(byNameResolver.getEnumClass());
        _lookupByName = byNameResolver.constructLookup();
        _enumsByIndex = byNameResolver.getRawEnums();
        _enumDefaultValue = byNameResolver.getDefaultValue();
        _caseInsensitive = caseInsensitive;
        _isFromIntValue = byNameResolver.isFromIntValue();
    }

    protected EnumDeserializer(EnumDeserializer base, Boolean caseInsensitive)
    {
        super(base);
        _lookupByName = base._lookupByName;
        _enumsByIndex = base._enumsByIndex;
        _enumDefaultValue = base._enumDefaultValue;
        _caseInsensitive = caseInsensitive;
        _isFromIntValue = base._isFromIntValue;
    }

    /**
     * Factory method used when Enum instances are to be deserialized
     * using a creator (static factory method)
     * 
     * @return Deserializer based on given factory method
     */
    public static ValueDeserializer<?> deserializerForCreator(DeserializationConfig config,
            Class<?> enumClass, AnnotatedMethod factory,
            ValueInstantiator valueInstantiator, SettableBeanProperty[] creatorProps)
    {
        if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(factory.getMember(),
                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
        }
        return new FactoryBasedEnumDeserializer(enumClass, factory,
                factory.getParameterType(0),
                valueInstantiator, creatorProps);
    }

    /**
     * Factory method used when Enum instances are to be deserialized
     * using a zero-/no-args factory method
     * 
     * @return Deserializer based on given no-args factory method
     */
    public static ValueDeserializer<?> deserializerForNoArgsCreator(DeserializationConfig config,
            Class<?> enumClass, AnnotatedMethod factory)
    {
        if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(factory.getMember(),
                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
        }
        return new FactoryBasedEnumDeserializer(enumClass, factory);
    }

    public EnumDeserializer withResolved(Boolean caseInsensitive) {
        if (Objects.equals(_caseInsensitive, caseInsensitive)) {
            return this;
        }
        return new EnumDeserializer(this, caseInsensitive);
    }
    
    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        Boolean caseInsensitive = findFormatFeature(ctxt, property, handledType(),
                JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
        if (caseInsensitive == null) {
            caseInsensitive = _caseInsensitive;
        }
        return withResolved(caseInsensitive);
    }

    /*
    /**********************************************************************
    /* Default ValueDeserializer implementation
    /**********************************************************************
     */

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Enum;
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return _enumDefaultValue;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // Usually should just get string value:
        // 04-Sep-2020, tatu: for 2.11.3 / 2.12.0, removed "FIELD_NAME" as allowed;
        //   did not work and gave odd error message.
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(p, ctxt, p.getText());
        }

        // But let's consider int acceptable as well (if within ordinal range)
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            // 26-Sep-2021, tatu: [databind#1850] Special case where we get "true" integer
            //    enumeration and should avoid use of {@code Enum.index()}
            if (_isFromIntValue) {
                // ... whether to rely on "getText()" returning String, or get number, convert?
                // For now assume all format backends can produce String:
                return _fromString(p, ctxt, p.getText());
            }
            return _fromInteger(p, ctxt, p.getIntValue());
        }
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (p.isExpectedStartObjectToken()) {
            return _fromString(p, ctxt,
                    ctxt.extractScalarFromObject(p, this, _valueClass));
        }
        return _deserializeOther(p, ctxt);
    }

    protected Object _fromString(JsonParser p, DeserializationContext ctxt,
            String text)
        throws JacksonException
    {
        CompactStringObjectMap lookup = ctxt.isEnabled(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                ? _getToStringLookup(ctxt) : _lookupByName;
        Object result = lookup.find(text);
        if (result == null) {
            String trimmed = text.trim();
            if ((trimmed == text) || (result = lookup.find(trimmed)) == null) {
                return _deserializeAltString(p, ctxt, lookup, trimmed);
            }
        }
        return result;
    }

    protected Object _fromInteger(JsonParser p, DeserializationContext ctxt,
            int index)
        throws JacksonException
    {
        final CoercionAction act = ctxt.findCoercionAction(logicalType(), handledType(),
                CoercionInputShape.Integer);

        // First, check legacy setting for slightly different message
        if (act == CoercionAction.Fail) {
            if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)) {
                return ctxt.handleWeirdNumberValue(_enumClass(), index,
                        "not allowed to deserialize Enum value out of number: disable DeserializationConfig.DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS to allow"
                        );
            }
            // otherwise this will force failure with new setting
            _checkCoercionFail(ctxt, act, handledType(), index,
                    "Integer value ("+index+")");
        }
        switch (act) {
        case AsNull:
            return null;
        case AsEmpty:
            return getEmptyValue(ctxt);
        case TryConvert:
        default:
        }
        if (index >= 0 && index < _enumsByIndex.length) {
            return _enumsByIndex[index];
        }
        if ((_enumDefaultValue != null)
                && ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)) {
            return _enumDefaultValue;
        }
        if (!ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            return ctxt.handleWeirdNumberValue(_enumClass(), index,
                    "index value outside legal index range [0..%s]",
                    _enumsByIndex.length-1);
        }
        return null;
    }
        /*
    return _checkCoercionFail(ctxt, act, rawTargetType, value,
            "empty String (\"\")");
            */
    
    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */
    
    private final Object _deserializeAltString(JsonParser p, DeserializationContext ctxt,
            CompactStringObjectMap lookup, String nameOrig)
        throws JacksonException
    {
        String name = nameOrig.trim();
        if (name.isEmpty()) { // empty or blank
            // 07-Jun-2021, tatu: [databind#3171] Need to consider Default value first
            //   (alas there's bit of duplication here)
            if ((_enumDefaultValue != null)
                    && ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)) {
                return _enumDefaultValue;
            }
            if (ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
                return null;
            }

            CoercionAction act;
            if (nameOrig.isEmpty()) {
                act = _findCoercionFromEmptyString(ctxt);
                act = _checkCoercionFail(ctxt, act, handledType(), nameOrig,
                        "empty String (\"\")");
            } else {
                act = _findCoercionFromBlankString(ctxt);
                act = _checkCoercionFail(ctxt, act, handledType(), nameOrig,
                        "blank String (all whitespace)");
            }
            switch (act) {
            case AsEmpty:
            case TryConvert:
                return getEmptyValue(ctxt);
            case AsNull:
            default: // Fail already handled earlier
            }
            return null;
//            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
        } else {
            // [databind#1313]: Case insensitive enum deserialization
            if (Boolean.TRUE.equals(_caseInsensitive)) {
                Object match = lookup.findCaseInsensitive(name);
                if (match != null) {
                    return match;
                }
            } else if (!ctxt.isEnabled(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                    && !_isFromIntValue) {
                // [databind#149]: Allow use of 'String' indexes as well -- unless prohibited (as per above)
                char c = name.charAt(0);
                if (c >= '0' && c <= '9') {
                    try {
                        int index = Integer.parseInt(name);
                        if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
                            return ctxt.handleWeirdStringValue(_enumClass(), name,
"value looks like quoted Enum index, but `DeserializationFeature.ALLOW_COERCION_OF_SCALARS` prevents use"
                                    );
                        }
                        if (index >= 0 && index < _enumsByIndex.length) {
                            return _enumsByIndex[index];
                        }
                    } catch (NumberFormatException e) {
                        // fine, ignore, was not an integer
                    }
                }
            }
        }
        if ((_enumDefaultValue != null)
                && ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)) {
            return _enumDefaultValue;
        }
        if (ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)) {
            return null;
        }
        return ctxt.handleWeirdStringValue(_enumClass(), name,
                "not one of the values accepted for Enum class: %s", lookup.keys());
    }

    protected Object _deserializeOther(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // [databind#381]
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return _deserializeFromArray(p, ctxt);
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    protected Class<?> _enumClass() {
        return handledType();
    }

    protected CompactStringObjectMap _getToStringLookup(DeserializationContext ctxt)
    {
        CompactStringObjectMap lookup = _lookupByToString;
        // note: exact locking not needed; all we care for here is to try to
        // reduce contention for the initial resolution
        if (lookup == null) {
            synchronized (this) {
                lookup = EnumResolver.constructUsingToString(ctxt.getConfig(), _enumClass())
                    .constructLookup();
            }
            _lookupByToString = lookup;
        }
        return lookup;
    }
}

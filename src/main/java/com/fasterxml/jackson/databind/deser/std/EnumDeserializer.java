package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.CompactStringObjectMap;
import com.fasterxml.jackson.databind.util.EnumResolver;
import java.util.Optional;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
@JacksonStdImpl
public class EnumDeserializer
    extends StdScalarDeserializer<Object>
    implements ContextualDeserializer
{
    private static final long serialVersionUID = 1L;

    protected Object[] _enumsByIndex;

    /**
     * @since 2.8
     */
    private final Enum<?> _enumDefaultValue;

    /**
     * @since 2.7.3
     */
    protected final CompactStringObjectMap _lookupByName;

    /**
     * Alternatively, we may need a different lookup object if "use toString"
     * is defined.
     *
     * @since 2.7.3
     */
    protected volatile CompactStringObjectMap _lookupByToString;

    protected final Boolean _caseInsensitive;

    private Boolean _useDefaultValueForUnknownEnum;
    private Boolean _useNullForUnknownEnum;

    /**
     * Marker flag for cases where we expect actual integral value for Enum,
     * based on {@code @JsonValue} (and equivalent) annotated accessor.
     *
     * @since 2.13
     */
    protected final boolean _isFromIntValue;

    /**
     * @since 2.9
     */
    public EnumDeserializer(EnumResolver byNameResolver, Boolean caseInsensitive)
    {
        super(byNameResolver.getEnumClass());
        _lookupByName = byNameResolver.constructLookup();
        _enumsByIndex = byNameResolver.getRawEnums();
        _enumDefaultValue = byNameResolver.getDefaultValue();
        _caseInsensitive = caseInsensitive;
        _isFromIntValue = byNameResolver.isFromIntValue();
    }

    /**
     * @since 2.15
     */
    protected EnumDeserializer(EnumDeserializer base, Boolean caseInsensitive,
            Boolean useDefaultValueForUnknownEnum, Boolean useNullForUnknownEnum)
    {
        super(base);
        _lookupByName = base._lookupByName;
        _enumsByIndex = base._enumsByIndex;
        _enumDefaultValue = base._enumDefaultValue;
        _caseInsensitive = caseInsensitive;
        _isFromIntValue = base._isFromIntValue;
        _useDefaultValueForUnknownEnum = useDefaultValueForUnknownEnum;
        _useNullForUnknownEnum = useNullForUnknownEnum;
    }

    /**
     * @since 2.9
     * @deprecated Since 2.15
     */
    @Deprecated
    protected EnumDeserializer(EnumDeserializer base, Boolean caseInsensitive) {
        this(base, caseInsensitive, null, null);
    }

    /**
     * @deprecated Since 2.9
     */
    @Deprecated
    public EnumDeserializer(EnumResolver byNameResolver) {
        this(byNameResolver, null);
    }

    /**
     * @deprecated Since 2.8
     */
    @Deprecated
    public static JsonDeserializer<?> deserializerForCreator(DeserializationConfig config,
            Class<?> enumClass, AnnotatedMethod factory) {
        return deserializerForCreator(config, enumClass, factory, null, null);
    }

    /**
     * Factory method used when Enum instances are to be deserialized
     * using a creator (static factory method)
     *
     * @return Deserializer based on given factory method
     *
     * @since 2.8
     */
    public static JsonDeserializer<?> deserializerForCreator(DeserializationConfig config,
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
     *
     * @since 2.8
     */
    public static JsonDeserializer<?> deserializerForNoArgsCreator(DeserializationConfig config,
            Class<?> enumClass, AnnotatedMethod factory)
    {
        if (config.canOverrideAccessModifiers()) {
            ClassUtil.checkAndFixAccess(factory.getMember(),
                    config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
        }
        return new FactoryBasedEnumDeserializer(enumClass, factory);
    }

    /**
     * @since 2.15
     */
    public EnumDeserializer withResolved(Boolean caseInsensitive,
            Boolean useDefaultValueForUnknownEnum, Boolean useNullForUnknownEnum) {
        if (Objects.equals(_caseInsensitive, caseInsensitive)
          && Objects.equals(_useDefaultValueForUnknownEnum, useDefaultValueForUnknownEnum)
          && Objects.equals(_useNullForUnknownEnum, useNullForUnknownEnum)) {
            return this;
        }
        return new EnumDeserializer(this, caseInsensitive, useDefaultValueForUnknownEnum, useNullForUnknownEnum);
    }

    /**
     * @since 2.9
     * @deprecated Since 2.15
     */
    @Deprecated
    public EnumDeserializer withResolved(Boolean caseInsensitive) {
        return withResolved(caseInsensitive,
                _useDefaultValueForUnknownEnum, _useNullForUnknownEnum);
    }

    @Override // since 2.9
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property) throws JsonMappingException
    {
        Boolean caseInsensitive = Optional.ofNullable(findFormatFeature(ctxt, property, handledType(),
          JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)).orElse(_caseInsensitive);
        Boolean useDefaultValueForUnknownEnum = Optional.ofNullable(findFormatFeature(ctxt, property, handledType(),
          JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)).orElse(_useDefaultValueForUnknownEnum);
        Boolean useNullForUnknownEnum = Optional.ofNullable(findFormatFeature(ctxt, property, handledType(),
          JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)).orElse(_useNullForUnknownEnum);
        return withResolved(caseInsensitive, useDefaultValueForUnknownEnum, useNullForUnknownEnum);
    }

    /*
    /**********************************************************
    /* Default JsonDeserializer implementation
    /**********************************************************
     */

    /**
     * Because of costs associated with constructing Enum resolvers,
     * let's cache instances by default.
     */
    @Override
    public boolean isCachable() { return true; }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Enum;
    }

    @Override // since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return _enumDefaultValue;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
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
        throws IOException
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
        throws IOException
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
        if (useDefaultValueForUnknownEnum(ctxt)) {
            return _enumDefaultValue;
        }
        if (!useNullForUnknownEnum(ctxt)) {
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
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private final Object _deserializeAltString(JsonParser p, DeserializationContext ctxt,
            CompactStringObjectMap lookup, String nameOrig) throws IOException
    {
        String name = nameOrig.trim();
        if (name.isEmpty()) { // empty or blank
            // 07-Jun-2021, tatu: [databind#3171] Need to consider Default value first
            //   (alas there's bit of duplication here)
            if (useDefaultValueForUnknownEnum(ctxt)) {
                return _enumDefaultValue;
            }
            if (useNullForUnknownEnum(ctxt)) {
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
"value looks like quoted Enum index, but `MapperFeature.ALLOW_COERCION_OF_SCALARS` prevents use"
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
        if (useDefaultValueForUnknownEnum(ctxt)) {
            return _enumDefaultValue;
        }
        if (useNullForUnknownEnum(ctxt)) {
            return null;
        }
        return ctxt.handleWeirdStringValue(_enumClass(), name,
                "not one of the values accepted for Enum class: %s",  lookup.keys());
    }

    protected Object _deserializeOther(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // [databind#381]
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return _deserializeFromArray(p, ctxt);
        }
        return ctxt.handleUnexpectedToken(_enumClass(), p);
    }

    protected Class<?> _enumClass() {
        return handledType();
    }

    protected CompactStringObjectMap _getToStringLookup(DeserializationContext ctxt) {
        CompactStringObjectMap lookup = _lookupByToString;
        if (lookup == null) {
            synchronized (this) {
                lookup = _lookupByToString;
                if (lookup == null) {
                    lookup = EnumResolver.constructUsingToString(ctxt.getConfig(), _enumClass())
                        .constructLookup();
                    _lookupByToString = lookup;
                }
            }
        }
        return lookup;
    }

    // @since 2.15
    protected boolean useNullForUnknownEnum(DeserializationContext ctxt) {
        return Boolean.TRUE.equals(_useNullForUnknownEnum)
          || ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }

    // @since 2.15
    protected boolean useDefaultValueForUnknownEnum(DeserializationContext ctxt) {
        return (_enumDefaultValue != null)
          && (Boolean.TRUE.equals(_useDefaultValueForUnknownEnum)
          || ctxt.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE));
    }
}

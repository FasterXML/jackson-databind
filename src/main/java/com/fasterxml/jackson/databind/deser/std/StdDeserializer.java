package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.io.NumberInput;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.impl.NullsAsEmptyProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.deser.impl.NullsFailProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Base class for common deserializers. Contains shared
 * base functionality for dealing with primitive values, such
 * as (re)parsing from String.
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
    implements java.io.Serializable,
        ValueInstantiator.Gettable // since 2.12
{
    private static final long serialVersionUID = 1L;

    /**
     * Bitmask that covers {@link DeserializationFeature#USE_BIG_INTEGER_FOR_INTS}
     * and {@link DeserializationFeature#USE_LONG_FOR_INTS}, used for more efficient
     * cheks when coercing integral values for untyped deserialization.
     *
     * @since 2.6
     */
    protected final static int F_MASK_INT_COERCIONS =
            DeserializationFeature.USE_BIG_INTEGER_FOR_INTS.getMask()
            | DeserializationFeature.USE_LONG_FOR_INTS.getMask();

    @Deprecated // since 2.12
    protected final static int F_MASK_ACCEPT_ARRAYS =
            DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS.getMask() |
            DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT.getMask();

    /**
     * Type of values this deserializer handles: sometimes
     * exact types, other time most specific supertype of
     * types deserializer handles (which may be as generic
     * as {@link Object} in some case)
     */
    final protected Class<?> _valueClass;

    final protected JavaType _valueType;

    protected StdDeserializer(Class<?> vc) {
        _valueClass = vc;
        _valueType = null;
    }

    protected StdDeserializer(JavaType valueType) {
        // 26-Sep-2017, tatu: [databind#1764] need to add null-check back until 3.x
        _valueClass = (valueType == null) ? Object.class : valueType.getRawClass();
        _valueType = valueType;
    }

    /**
     * Copy-constructor for sub-classes to use, most often when creating
     * new instances for {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer}.
     *
     * @since 2.5
     */
    protected StdDeserializer(StdDeserializer<?> src) {
        _valueClass = src._valueClass;
        _valueType = src._valueType;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public Class<?> handledType() { return _valueClass; }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    /**
     * @deprecated Since 2.3 use {@link #handledType} instead
     */
    @Deprecated
    public final Class<?> getValueClass() { return _valueClass; }

    /**
     * Exact structured type this deserializer handles, if known.
     */
    public JavaType getValueType() { return _valueType; }

    /**
     * Convenience method for getting handled type as {@link JavaType}, regardless
     * of whether deserializer has one already resolved (and accessible via
     * {@link #getValueType()}) or not: equivalent to:
     *<pre>
     *   if (getValueType() != null) {
     *        return getValueType();
     *   }
     *   return ctxt.constructType(handledType());
     *</pre>
     *
     * @since 2.10
     */
    public JavaType getValueType(DeserializationContext ctxt) {
        if (_valueType != null) {
            return _valueType;
        }
        return ctxt.constructType(_valueClass);
    }

    /**
     * @since 2.12
     */
    @Override // for ValueInstantiator.Gettable
    public ValueInstantiator getValueInstantiator() { return null; }

    /**
     * Method that can be called to determine if given deserializer is the default
     * deserializer Jackson uses; as opposed to a custom deserializer installed by
     * a module or calling application. Determination is done using
     * {@link JacksonStdImpl} annotation on deserializer class.
     */
    protected boolean isDefaultDeserializer(JsonDeserializer<?> deserializer) {
        return ClassUtil.isJacksonStdImpl(deserializer);
    }

    protected boolean isDefaultKeyDeserializer(KeyDeserializer keyDeser) {
        return ClassUtil.isJacksonStdImpl(keyDeser);
    }

    /*
    /**********************************************************
    /* Partial JsonDeserializer implementation
    /**********************************************************
     */

    /**
     * Base implementation that does not assume specific type
     * inclusion mechanism. Sub-classes are expected to override
     * this method if they are to handle type information.
     */
    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }

    /*
    /**********************************************************************
    /* High-level handling of secondary input shapes (with possible coercion)
    /**********************************************************************
     */

    /**
     * Helper method that allows easy support for array-related coercion features:
     * checks for either empty array, or single-value array-wrapped value (if coercion
     * enabled by {@code CoercionConfigs} (since 2.12), and either reports
     * an exception (if no coercion allowed), or returns appropriate
     * result value using coercion mechanism indicated.
     *<p>
     * This method should NOT be called if Array representation is explicitly supported
     * for type: it should only be called in case it is otherwise unrecognized.
     *<p>
     * NOTE: in case of unwrapped single element, will handle actual decoding
     * by calling {@link #_deserializeWrappedValue}, which by default calls
     * {@link #deserialize(JsonParser, DeserializationContext)}.
     *
     * @since 2.9
     */
    @SuppressWarnings("unchecked")
    protected T _deserializeFromArray(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        final CoercionAction act = _findCoercionFromEmptyArray(ctxt);
        final boolean unwrap = ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        if (unwrap || (act != CoercionAction.Fail)) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                switch (act) {
                case AsEmpty:
                    return (T) getEmptyValue(ctxt);
                case AsNull:
                case TryConvert:
                    return getNullValue(ctxt);
                default:
                }
            } else if (unwrap) {
                final T parsed = _deserializeWrappedValue(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;
            }
        }
        return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), JsonToken.START_ARRAY, p, null);
    }

    /**
     * Helper method that may be used to support fallback for Empty String / Empty Array
     * non-standard representations; usually for things serialized as JSON Objects.
     *
     * @since 2.5
     *
     * @deprecated Since 2.12
     */
    @SuppressWarnings("unchecked")
    @Deprecated // since 2.12
    protected T _deserializeFromEmpty(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (p.hasToken(JsonToken.START_ARRAY)) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
                JsonToken t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }
        }
        return (T) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /**
     * Helper method to call in case deserializer does not support native automatic
     * use of incoming String values, but there may be standard coercions to consider.
     *
     * @since 2.12
     */
    @SuppressWarnings("unchecked")
    protected T _deserializeFromString(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        final ValueInstantiator inst = getValueInstantiator();
        final Class<?> rawTargetType = handledType();
        String value = p.getValueAsString();

        if ((inst != null) && inst.canCreateFromString()) {
            return (T) inst.createFromString(ctxt, value);
        }
        if (value.isEmpty()) {
            final CoercionAction act = ctxt.findCoercionAction(logicalType(), rawTargetType,
                    CoercionInputShape.EmptyString);
            return (T) _deserializeFromEmptyString(p, ctxt, act, rawTargetType,
                    "empty String (\"\")");
        }
        if (_isBlank(value)) {
            final CoercionAction act = ctxt.findCoercionFromBlankString(logicalType(), rawTargetType,
                    CoercionAction.Fail);
            return (T) _deserializeFromEmptyString(p, ctxt, act, rawTargetType,
                    "blank String (all whitespace)");
        }

        // 28-Sep-2011, tatu: Ok this is not clean at all; but since there are legacy
        //   systems that expect conversions in some cases, let's just add a minimal
        //   patch (note: same could conceivably be used for numbers too).
        if (inst != null) {
            value = value.trim(); // mostly to avoid problems wrt XML indentation
            if (inst.canCreateFromInt()) {
                if (ctxt.findCoercionAction(LogicalType.Integer, Integer.class,
                        CoercionInputShape.String) == CoercionAction.TryConvert) {
                    return (T) inst.createFromInt(ctxt, _parseIntPrimitive(ctxt, value));
                }
            }
            if (inst.canCreateFromLong()) {
                if (ctxt.findCoercionAction(LogicalType.Integer, Long.class,
                        CoercionInputShape.String) == CoercionAction.TryConvert) {
                    return (T) inst.createFromLong(ctxt, _parseLongPrimitive(ctxt, value));
                }
            }
            if (inst.canCreateFromBoolean()) {
                // 29-May-2020, tatu: With 2.12 can and should use CoercionConfig so:
                if (ctxt.findCoercionAction(LogicalType.Boolean, Boolean.class,
                        CoercionInputShape.String) == CoercionAction.TryConvert) {
                    String str = value.trim();
                    if ("true".equals(str)) {
                        return (T) inst.createFromBoolean(ctxt, true);
                    }
                    if ("false".equals(str)) {
                        return (T) inst.createFromBoolean(ctxt, false);
                    }
                }
            }
        }
        return (T) ctxt.handleMissingInstantiator(rawTargetType, inst, ctxt.getParser(),
                "no String-argument constructor/factory method to deserialize from String value ('%s')",
                value);
    }

    protected Object _deserializeFromEmptyString(JsonParser p, DeserializationContext ctxt,
            CoercionAction act, Class<?> rawTargetType, String desc) throws IOException
    {

        switch (act) {
        case AsEmpty:
            return getEmptyValue(ctxt);
        case Fail:
            // This will throw an exception
            _checkCoercionFail(ctxt, act, rawTargetType, "", "empty String (\"\")");
            // ... so will never fall through
        case TryConvert:
            // hmmmh... empty or null, typically? Assume "as null" for now
        case AsNull:
        default:
            return null;
        }

        // 06-Nov-2020, tatu: This was behavior pre-2.12, giving less useful
        //    exception
        /*
        final ValueInstantiator inst = getValueInstantiator();

        // 03-Jun-2020, tatu: Should ideally call `handleUnexpectedToken()` instead, but
        //    since this call was already made, use it.
        return ctxt.handleMissingInstantiator(rawTargetType, inst, p,
"Cannot deserialize value of type %s from %s (no String-argument constructor/factory method; coercion not enabled)",
                ClassUtil.getTypeDescription(getValueType(ctxt)), desc);
                */
    }

    /**
     * Helper called to support {@link DeserializationFeature#UNWRAP_SINGLE_VALUE_ARRAYS}:
     * default implementation simply calls
     * {@link #deserialize(JsonParser, DeserializationContext)},
     * but handling may be overridden.
     *
     * @since 2.9
     */
    protected T _deserializeWrappedValue(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // 23-Mar-2017, tatu: Let's specifically block recursive resolution to avoid
        //   either supporting nested arrays, or to cause infinite looping.
        if (p.hasToken(JsonToken.START_ARRAY)) {
            @SuppressWarnings("unchecked")
            T result = (T) handleNestedArrayForSingle(p, ctxt);
            return result;
        }
        return (T) deserialize(p, ctxt);
    }

    /*
    /**********************************************************
    /* Helper methods for sub-classes, parsing: while mostly
    /* useful for numeric types, can be also useful for dealing
    /* with things serialized as numbers (such as Dates).
    /**********************************************************
     */

    @Deprecated // since 2.12, use overloaded variant that does NOT take target type
    protected final boolean _parseBooleanPrimitive(DeserializationContext ctxt,
            JsonParser p, Class<?> targetType) throws IOException {
        return _parseBooleanPrimitive(p, ctxt);
    }

    /**
     * @param ctxt Deserialization context for accessing configuration
     * @param p Underlying parser
     */
    protected final boolean _parseBooleanPrimitive(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_INT:
            // may accept ints too, (0 == false, otherwise true)

            // call returns `null`, Boolean.TRUE or Boolean.FALSE so:
            return Boolean.TRUE.equals(_coerceBooleanFromInt(p, ctxt, Boolean.TYPE));
        case JsonTokenId.ID_TRUE: // usually caller should have handled but:
            return true;
        case JsonTokenId.ID_FALSE:
            return false;
        case JsonTokenId.ID_NULL: // null fine for non-primitive
            _verifyNullForPrimitive(ctxt);
            return false;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Boolean.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            // 12-Jun-2020, tatu: For some reason calling `_deserializeFromArray()` won't work so:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (boolean) handleNestedArrayForSingle(p, ctxt);
                }
                final boolean parsed = _parseBooleanPrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through
        default:
            return ((Boolean) ctxt.handleUnexpectedToken(Boolean.TYPE, p)).booleanValue();
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Boolean, Boolean.TYPE);
        if (act == CoercionAction.AsNull) {
            _verifyNullForPrimitive(ctxt);
            return false;
        }
        if (act == CoercionAction.AsEmpty) {
            return false;
        }
        text = text.trim();
        final int len = text.length();

        // For [databind#1852] allow some case-insensitive matches (namely,
        // true/True/TRUE, false/False/FALSE
        if (len == 4) {
            if (_isTrue(text)) {
                return true;
            }
        } else if (len == 5) {
            if (_isFalse(text)) {
                return false;
            }
        }
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return false;
        }
        Boolean b = (Boolean) ctxt.handleWeirdStringValue(Boolean.TYPE, text,
                "only \"true\"/\"True\"/\"TRUE\" or \"false\"/\"False\"/\"FALSE\" recognized");
        return Boolean.TRUE.equals(b);
    }

    // [databind#1852]
    protected boolean _isTrue(String text) {
        char c = text.charAt(0);
        if (c == 't') {
            return "true".equals(text);
        }
        if (c == 'T') {
            return "TRUE".equals(text) || "True".equals(text);
        }
        return false;
    }

    protected boolean _isFalse(String text) {
        char c = text.charAt(0);
        if (c == 'f') {
            return "false".equals(text);
        }
        if (c == 'F') {
            return "FALSE".equals(text) || "False".equals(text);
        }
        return false;
    }

    /**
     * Helper method called for cases where non-primitive, boolean-based value
     * is to be deserialized: result of this method will be {@link java.lang.Boolean},
     * although actual target type may be something different.
     *<p>
     * Note: does NOT dynamically access "empty value" or "null value" of deserializer
     * since those values could be of type other than {@link java.lang.Boolean}.
     * Caller may need to translate from 3 possible result types into appropriately
     * matching output types.
     *
     * @param p Underlying parser
     * @param ctxt Deserialization context for accessing configuration
     * @param targetType Actual type that is being deserialized, may be
     *    same as {@link #handledType} but could be {@code AtomicBoolean} for example.
     *    Used for coercion config access.
     *
     * @since 2.12
     */
    protected final Boolean _parseBoolean(JsonParser p, DeserializationContext ctxt,
            Class<?> targetType)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_INT:
            // may accept ints too, (0 == false, otherwise true)
            return _coerceBooleanFromInt(p, ctxt, targetType);
        case JsonTokenId.ID_TRUE:
            return true;
        case JsonTokenId.ID_FALSE:
            return false;
        case JsonTokenId.ID_NULL: // null fine for non-primitive
            return null;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, targetType);
            break;
        case JsonTokenId.ID_START_ARRAY: // unwrapping / from-empty-array coercion?
            return (Boolean) _deserializeFromArray(p, ctxt);
        default:
            return (Boolean) ctxt.handleUnexpectedToken(targetType, p);
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Boolean, targetType);
        if (act == CoercionAction.AsNull) {
            return null;
        }
        if (act == CoercionAction.AsEmpty) {
            return false;
        }
        text = text.trim();
        final int len = text.length();

        // For [databind#1852] allow some case-insensitive matches (namely,
        // true/True/TRUE, false/False/FALSE
        if (len == 4) {
            if (_isTrue(text)) {
                return true;
            }
        } else if (len == 5) {
            if (_isFalse(text)) {
                return false;
            }
        }
        if (_checkTextualNull(ctxt, text)) {
            return null;
        }
        return (Boolean) ctxt.handleWeirdStringValue(targetType, text,
                "only \"true\" or \"false\" recognized");
    }

    protected final byte _parseBytePrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            CoercionAction act = _checkFloatToIntCoercion(p, ctxt, Byte.TYPE);
            if (act == CoercionAction.AsNull) {
                return (byte) 0;
            }
            if (act == CoercionAction.AsEmpty) {
                return (byte) 0;
            }
            return p.getByteValue();
        case JsonTokenId.ID_NUMBER_INT:
            return p.getByteValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return (byte) 0;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Byte.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY: // unwrapping / from-empty-array coercion?
            // 12-Jun-2020, tatu: For some reason calling `_deserializeFromArray()` won't work so:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (byte) handleNestedArrayForSingle(p, ctxt);
                }
                final byte parsed = _parseBytePrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through
        default:
            return ((Byte) ctxt.handleUnexpectedToken(ctxt.constructType(Byte.TYPE), p)).byteValue();
        }

        // Coercion from String
        CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Byte.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return (byte) 0;
        }
        if (act == CoercionAction.AsEmpty) {
            return (byte) 0;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return (byte) 0;
        }
        int value;
        try {
            value = NumberInput.parseInt(text);
        } catch (IllegalArgumentException iae) {
            return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid `byte` value");
        }
        // So far so good: but does it fit? Allow both -128 / 255 range (inclusive)
        if (_byteOverflow(value)) {
            return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                    "overflow, value cannot be represented as 8-bit value");
        }
        return (byte) value;
    }

    protected final short _parseShortPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            CoercionAction act = _checkFloatToIntCoercion(p, ctxt, Short.TYPE);
            if (act == CoercionAction.AsNull) {
                return (short) 0;
            }
            if (act == CoercionAction.AsEmpty) {
                return (short) 0;
            }
            return p.getShortValue();
        case JsonTokenId.ID_NUMBER_INT:
            return p.getShortValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return (short) 0;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Short.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            // 12-Jun-2020, tatu: For some reason calling `_deserializeFromArray()` won't work so:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (short) handleNestedArrayForSingle(p, ctxt);
                }
                final short parsed = _parseShortPrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through to fail
        default:
            return ((Short) ctxt.handleUnexpectedToken(ctxt.constructType(Short.TYPE), p)).shortValue();
        }

        CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Short.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return (short) 0;
        }
        if (act == CoercionAction.AsEmpty) {
            return (short) 0;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return (short) 0;
        }
        int value;
        try {
            value = NumberInput.parseInt(text);
        } catch (IllegalArgumentException iae) {
            return (Short) ctxt.handleWeirdStringValue(Short.TYPE, text,
                    "not a valid `short` value");
        }
        if (_shortOverflow(value)) {
            return (Short) ctxt.handleWeirdStringValue(Short.TYPE, text,
                    "overflow, value cannot be represented as 16-bit value");
        }
        return (short) value;
    }

    protected final int _parseIntPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, Integer.TYPE);
            if (act == CoercionAction.AsNull) {
                return 0;
            }
            if (act == CoercionAction.AsEmpty) {
                return 0;
            }
            return p.getValueAsInt();
        case JsonTokenId.ID_NUMBER_INT:
            return p.getIntValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return 0;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Integer.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (int) handleNestedArrayForSingle(p, ctxt);
                }
                final int parsed = _parseIntPrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through to fail
        default:
            return ((Number) ctxt.handleUnexpectedToken(Integer.TYPE, p)).intValue();
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Integer.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return 0;
        }
        if (act == CoercionAction.AsEmpty) {
            return 0;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return 0;
        }
        return _parseIntPrimitive(ctxt, text);
    }

    /**
     * @since 2.9
     */
    protected final int _parseIntPrimitive(DeserializationContext ctxt, String text) throws IOException
    {
        try {
            if (text.length() > 9) {
                long l = NumberInput.parseLong(text);
                if (_intOverflow(l)) {
                    Number v = (Number) ctxt.handleWeirdStringValue(Integer.TYPE, text,
                        "Overflow: numeric value (%s) out of range of int (%d -%d)",
                        text, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    return _nonNullNumber(v).intValue();
                }
                return (int) l;
            }
            return NumberInput.parseInt(text);
        } catch (IllegalArgumentException iae) {
            Number v = (Number) ctxt.handleWeirdStringValue(Integer.TYPE, text,
                    "not a valid `int` value");
            return _nonNullNumber(v).intValue();
        }
    }

    /**
     * @since 2.12
     */
    protected final Integer _parseInteger(JsonParser p, DeserializationContext ctxt,
            Class<?> targetType)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT: // coercing may work too
            final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, targetType);
            if (act == CoercionAction.AsNull) {
                return (Integer) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Integer) getEmptyValue(ctxt);
            }
            return p.getValueAsInt();
        case JsonTokenId.ID_NUMBER_INT: // NOTE: caller assumed to check in fast path
            return p.getIntValue();
        case JsonTokenId.ID_NULL: // null fine for non-primitive
            return (Integer) getNullValue(ctxt);
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, targetType);
            break;
        case JsonTokenId.ID_START_ARRAY:
            return (Integer) _deserializeFromArray(p, ctxt);
        default:
            return (Integer) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text);
        if (act == CoercionAction.AsNull) {
            return (Integer) getNullValue(ctxt);
        }
        if (act == CoercionAction.AsEmpty) {
            return (Integer) getEmptyValue(ctxt);
        }
        text = text.trim();
        if (_checkTextualNull(ctxt, text)) {
            return (Integer) getNullValue(ctxt);
        }
        return _parseInteger(ctxt, text);
    }

    /**
     * @since 2.14
     */
    protected final Integer _parseInteger(DeserializationContext ctxt, String text) throws IOException
    {
        try {
            if (text.length() > 9) {
                long l = NumberInput.parseLong(text);
                if (_intOverflow(l)) {
                    return (Integer) ctxt.handleWeirdStringValue(Integer.class, text,
                        "Overflow: numeric value (%s) out of range of `java.lang.Integer` (%d -%d)",
                        text, Integer.MIN_VALUE, Integer.MAX_VALUE);
                }
                return Integer.valueOf((int) l);
            }
            return NumberInput.parseInt(text);
        } catch (IllegalArgumentException iae) {
            return(Integer) ctxt.handleWeirdStringValue(Integer.class, text,
                    "not a valid `java.lang.Integer` value");
        }
    }

    protected final long _parseLongPrimitive(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, Long.TYPE);
            if (act == CoercionAction.AsNull) {
                return 0L;
            }
            if (act == CoercionAction.AsEmpty) {
                return 0L;
            }
            return p.getValueAsLong();
        case JsonTokenId.ID_NUMBER_INT:
            return p.getLongValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return 0L;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Long.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (long) handleNestedArrayForSingle(p, ctxt);
                }
                final long parsed = _parseLongPrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through
        default:
            return ((Number) ctxt.handleUnexpectedToken(Long.TYPE, p)).longValue();
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Long.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return 0L;
        }
        if (act == CoercionAction.AsEmpty) {
            return 0L;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return 0L;
        }
        return _parseLongPrimitive(ctxt, text);
    }

    /**
     * @since 2.9
     */
    protected final long _parseLongPrimitive(DeserializationContext ctxt, String text) throws IOException
    {
        try {
            return NumberInput.parseLong(text);
        } catch (IllegalArgumentException iae) { }
        {
            Number v = (Number) ctxt.handleWeirdStringValue(Long.TYPE, text,
                    "not a valid `long` value");
            return _nonNullNumber(v).longValue();
        }
    }

    /**
     * @since 2.12
     */
    protected final Long _parseLong(JsonParser p, DeserializationContext ctxt,
            Class<?> targetType)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, targetType);
            if (act == CoercionAction.AsNull) {
                return (Long) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Long) getEmptyValue(ctxt);
            }
            return p.getValueAsLong();
        case JsonTokenId.ID_NULL: // null fine for non-primitive
            return (Long) getNullValue(ctxt);
        case JsonTokenId.ID_NUMBER_INT:
            return p.getLongValue();
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, targetType);
            break;
        case JsonTokenId.ID_START_ARRAY:
            return (Long) _deserializeFromArray(p, ctxt);
        default:
            return (Long) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text);
        if (act == CoercionAction.AsNull) {
            return (Long) getNullValue(ctxt);
        }
        if (act == CoercionAction.AsEmpty) {
            return (Long) getEmptyValue(ctxt);
        }
        text = text.trim();
        if (_checkTextualNull(ctxt, text)) {
            return (Long) getNullValue(ctxt);
        }
        // let's allow Strings to be converted too
        return _parseLong(ctxt, text);
    }

    /**
     * @since 2.14
     */
    protected final Long _parseLong(DeserializationContext ctxt, String text) throws IOException
    {
        try {
            return NumberInput.parseLong(text);
        } catch (IllegalArgumentException iae) { }
        return (Long) ctxt.handleWeirdStringValue(Long.class, text,
                "not a valid `java.lang.Long` value");
    }

    protected final float _parseFloatPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_INT:
            final CoercionAction act = _checkIntToFloatCoercion(p, ctxt, Float.TYPE);
            if (act == CoercionAction.AsNull) {
                return 0.0f;
            }
            if (act == CoercionAction.AsEmpty) {
                return 0.0f;
            }
            // fall through to coerce
        case JsonTokenId.ID_NUMBER_FLOAT:
            return p.getFloatValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return 0f;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Float.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (float) handleNestedArrayForSingle(p, ctxt);
                }
                final float parsed = _parseFloatPrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through
        default:
            return ((Number) ctxt.handleUnexpectedToken(Float.TYPE, p)).floatValue();
        }

        // 18-Nov-2020, tatu: Special case, Not-a-Numbers as String need to be
        //     considered "native" representation as JSON does not allow as numbers,
        //     and hence not bound by coercion rules
        {
            Float nan = _checkFloatSpecialValue(text);
            if (nan != null) {
                return nan.floatValue();
            }
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Float.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return  0.0f;
        }
        if (act == CoercionAction.AsEmpty) {
            return  0.0f;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return  0.0f;
        }
        return _parseFloatPrimitive(p, ctxt, text);
}

    /**
     * @since 2.9
     */
    protected final float _parseFloatPrimitive(DeserializationContext ctxt, String text)
        throws IOException
    {
        try {
            return NumberInput.parseFloat(text);
        } catch (IllegalArgumentException iae) { }
        Number v = (Number) ctxt.handleWeirdStringValue(Float.TYPE, text,
                "not a valid `float` value");
        return _nonNullNumber(v).floatValue();
    }

    /**
     * @since 2.14
     */
    protected final float _parseFloatPrimitive(JsonParser p, DeserializationContext ctxt, String text)
            throws IOException
    {
        try {
            return NumberInput.parseFloat(text, p.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        } catch (IllegalArgumentException iae) { }
        Number v = (Number) ctxt.handleWeirdStringValue(Float.TYPE, text,
                "not a valid `float` value");
        return _nonNullNumber(v).floatValue();
    }

    /**
     * Helper method called to check whether given String value contains one of
     * "special" values (currently, NaN ("not-a-number") and plus/minus Infinity)
     * and if so, returns that value; otherwise returns {@code null}.
     *
     * @param text String value to check
     *
     * @return One of {@link Float} constants referring to special value decoded,
     *   if value matched; {@code null} otherwise.
     *
     * @since 2.12
     */
    protected Float _checkFloatSpecialValue(String text)
    {
        if (!text.isEmpty()) {
            switch (text.charAt(0)) {
            case 'I':
                if (_isPosInf(text)) {
                    return Float.POSITIVE_INFINITY;
                }
                break;
            case 'N':
                if (_isNaN(text)) { return Float.NaN; }
                break;
            case '-':
                if (_isNegInf(text)) {
                    return Float.NEGATIVE_INFINITY;
                }
                break;
            default:
            }
        }
        return null;
    }

    protected final double _parseDoublePrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_INT:
            final CoercionAction act = _checkIntToFloatCoercion(p, ctxt, Double.TYPE);
            if (act == CoercionAction.AsNull) {
                return 0.0d;
            }
            if (act == CoercionAction.AsEmpty) {
                return 0.0d;
            }
            // fall through to coerce
        case JsonTokenId.ID_NUMBER_FLOAT:
            return p.getDoubleValue();
        case JsonTokenId.ID_NULL:
            _verifyNullForPrimitive(ctxt);
            return 0.0;
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, Double.TYPE);
            break;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                if (p.nextToken() == JsonToken.START_ARRAY) {
                    return (double) handleNestedArrayForSingle(p, ctxt);
                }
                final double parsed = _parseDoublePrimitive(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            // fall through
        default:
            return ((Number) ctxt.handleUnexpectedToken(Double.TYPE, p)).doubleValue();
        }

        // 18-Nov-2020, tatu: Special case, Not-a-Numbers as String need to be
        //     considered "native" representation as JSON does not allow as numbers,
        //     and hence not bound by coercion rules
        {
            Double nan = this._checkDoubleSpecialValue(text);
            if (nan != null) {
                return nan.doubleValue();
            }
        }

        final CoercionAction act = _checkFromStringCoercion(ctxt, text,
                LogicalType.Integer, Double.TYPE);
        if (act == CoercionAction.AsNull) {
            // 03-May-2021, tatu: Might not be allowed (should we do "empty" check?)
            _verifyNullForPrimitive(ctxt);
            return  0.0;
        }
        if (act == CoercionAction.AsEmpty) {
            return  0.0;
        }
        text = text.trim();
        if (_hasTextualNull(text)) {
            _verifyNullForPrimitiveCoercion(ctxt, text);
            return  0.0;
        }
        return _parseDoublePrimitive(p, ctxt, text);
    }

    /**
     * @since 2.9
     */
    protected final double _parseDoublePrimitive(DeserializationContext ctxt, String text)
        throws IOException
    {
        try {
            return _parseDouble(text);
        } catch (IllegalArgumentException iae) { }
        Number v = (Number) ctxt.handleWeirdStringValue(Double.TYPE, text,
                "not a valid `double` value (as String to convert)");
        return _nonNullNumber(v).doubleValue();
    }

    /**
     * @since 2.14
     */
    protected final double _parseDoublePrimitive(JsonParser p, DeserializationContext ctxt, String text)
            throws IOException
    {
        try {
            return _parseDouble(text, p.isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
        } catch (IllegalArgumentException iae) { }
        Number v = (Number) ctxt.handleWeirdStringValue(Double.TYPE, text,
                "not a valid `double` value (as String to convert)");
        return _nonNullNumber(v).doubleValue();
    }

    /**
     * Helper method for encapsulating calls to low-level double value parsing; single place
     * just because we need a work-around that must be applied to all calls.
     * Does not use the new <code>useFastParser</code> support.
     *
     * @see #_parseDouble(String, boolean)
     */
    protected final static double _parseDouble(final String numStr) throws NumberFormatException
    {
        return _parseDouble(numStr, false);
    }

    /**
     * Helper method for encapsulating calls to low-level double value parsing; single place
     * just because we need a work-around that must be applied to all calls.
     *
     * @since 2.14
     */
    protected final static double _parseDouble(final String numStr, final boolean useFastParser) throws NumberFormatException
    {
        return NumberInput.parseDouble(numStr, useFastParser);
    }

    /**
     * Helper method called to check whether given String value contains one of
     * "special" values (currently, NaN ("not-a-number") and plus/minus Infinity)
     * and if so, returns that value; otherwise returns {@code null}.
     *
     * @param text String value to check
     *
     * @return One of {@link Double} constants referring to special value decoded,
     *   if value matched; {@code null} otherwise.
     *
     * @since 2.12
     */
    protected Double _checkDoubleSpecialValue(String text)
    {
        if (!text.isEmpty()) {
            switch (text.charAt(0)) {
            case 'I':
                if (_isPosInf(text)) {
                    return Double.POSITIVE_INFINITY;
                }
                break;
            case 'N':
                if (_isNaN(text)) {
                    return Double.NaN;
                }
                break;
            case '-':
                if (_isNegInf(text)) {
                    return Double.NEGATIVE_INFINITY;
                }
                break;
            default:
            }
        }
        return null;
    }

    protected java.util.Date _parseDate(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        String text;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            text = p.getText();
            break;
        case JsonTokenId.ID_NUMBER_INT:
            {
                long ts;
                try {
                    ts = p.getLongValue();
                // 16-Jan-2019, tatu: 2.10 uses InputCoercionException, earlier StreamReadException
                } catch (StreamReadException e) {
                    Number v = (Number) ctxt.handleWeirdNumberValue(_valueClass, p.getNumberValue(),
                            "not a valid 64-bit `long` for creating `java.util.Date`");
                    ts = v.longValue();
                }
                return new java.util.Date(ts);
            }
        case JsonTokenId.ID_NULL:
            return (java.util.Date) getNullValue(ctxt);
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            text = ctxt.extractScalarFromObject(p, this, _valueClass);
            break;
        case JsonTokenId.ID_START_ARRAY:
            return _parseDateFromArray(p, ctxt);
        default:
            return (java.util.Date) ctxt.handleUnexpectedToken(_valueClass, p);
        }

        return _parseDate(text.trim(), ctxt);
    }

    // @since 2.9
    protected java.util.Date _parseDateFromArray(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        final CoercionAction act = _findCoercionFromEmptyArray(ctxt);
        final boolean unwrap = ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        if (unwrap || (act != CoercionAction.Fail)) {
            JsonToken t = p.nextToken();
            if (t == JsonToken.END_ARRAY) {
                switch (act) {
                case AsEmpty:
                    return (java.util.Date) getEmptyValue(ctxt);
                case AsNull:
                case TryConvert:
                    return (java.util.Date) getNullValue(ctxt);
                default:
                }
            } else if (unwrap) {
                if (t == JsonToken.START_ARRAY) {
                    return (java.util.Date) handleNestedArrayForSingle(p, ctxt);
                }
                final Date parsed = _parseDate(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
        }
        return (java.util.Date) ctxt.handleUnexpectedToken(_valueClass, JsonToken.START_ARRAY, p, null);
    }

    /**
     * @since 2.8
     */
    protected java.util.Date _parseDate(String value, DeserializationContext ctxt)
        throws IOException
    {
        try {
            // Take empty Strings to mean 'empty' Value, usually 'null':
            if (value.isEmpty()) {
                final CoercionAction act = _checkFromStringCoercion(ctxt, value);
                switch (act) { // note: Fail handled above
                case AsEmpty:
                    return new java.util.Date(0L);
                case AsNull:
                case TryConvert:
                default:
                }
                return null;
            }
            // 10-Jun-2020, tatu: Legacy handling from pre-2.12... should we still have it?
            if (_hasTextualNull(value)) {
                return null;
            }
            return ctxt.parseDate(value);
        } catch (IllegalArgumentException iae) {
            return (java.util.Date) ctxt.handleWeirdStringValue(_valueClass, value,
                    "not a valid representation (error: %s)",
                    ClassUtil.exceptionMessage(iae));
        }
    }

    /**
     * @since 2.1
     * @deprecated Since 2.14 (use the non-deprecated overload)
     */
    @Deprecated
    protected final String _parseString(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        return _parseString(p, ctxt,
                // Note: could consider passing `this`, but since this methods is
                // often called for values "inside" type we are handling (like
                // String array or Collection of Strings) that would probably
                // not match. So default to "nulls as nulls" handler.
                NullsConstantProvider.nuller());
    }

    /**
     * Helper method used for deserializing String value, if possible, doing
     * necessary conversion or throwing exception as necessary.
     *<p>
     * Starting with 2.14 will also start checking {@code CoercionConfig}
     * configuration to check whether coercion needed is allowed.
     *
     * @param p Currently active parser being iterated over
     * @param ctxt Deserialization context
     * @param nullProvider Entity we (only) need for case of secondary value
     *    type being coerced into {@code null}: if so, provider is asked for
     *    possible "null replacement" value.
     *
     * @since 2.14 (with this signature)
     */
    protected final String _parseString(JsonParser p, DeserializationContext ctxt,
            NullValueProvider nullProvider)
        throws IOException
    {
        CoercionAction act = CoercionAction.TryConvert;
        // 05-Jan-2022, tatu: We are usually coercing into `String` as element,
        //    or such; `_valueClass` would be wrong choice here
        final Class<?> rawTargetType = String.class;

        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            return p.getText();
        // 07-Nov-2019, tatu: [databind#2535] Need to support byte[]->Base64 same as `StringDeserializer`
        case JsonTokenId.ID_EMBEDDED_OBJECT:
            Object ob = p.getEmbeddedObject();
            if (ob instanceof byte[]) {
                return ctxt.getBase64Variant().encode((byte[]) ob, false);
            }
            if (ob == null) {
                return null;
            }
            // otherwise, try conversion using toString()...
            return ob.toString();
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        case JsonTokenId.ID_START_OBJECT:
            return ctxt.extractScalarFromObject(p, this, _valueClass);
        case JsonTokenId.ID_NUMBER_INT:
            act = _checkIntToStringCoercion(p, ctxt, rawTargetType);
            break;
        case JsonTokenId.ID_NUMBER_FLOAT:
            act = _checkFloatToStringCoercion(p, ctxt, rawTargetType);
            break;
        case JsonTokenId.ID_TRUE:
        case JsonTokenId.ID_FALSE:
            act = _checkBooleanToStringCoercion(p, ctxt, rawTargetType);
            break;
        }

        // !!! 05-Jan-2022, tatu: This might work in practice, but in theory
        //   we should not use "nullProvider" of this deserializer as this
        //   unlikely to be what we want (it's for containing type, not String)
        if (act == CoercionAction.AsNull) {
            return (String) nullProvider.getNullValue(ctxt);
        }
        if (act == CoercionAction.AsEmpty) {
            return "";
        }

        // allow coercions for other scalar types
        // 17-Jan-2018, tatu: Related to [databind#1853] avoid FIELD_NAME by ensuring it's
        //   "real" scalar
        if (p.currentToken().isScalarValue()) {
            String text = p.getValueAsString();
            if (text != null) {
                return text;
            }
        }
        return (String) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    /**
     * Helper method called to determine if we are seeing String value of
     * "null", and, further, that it should be coerced to null just like
     * null token.
     *
     * @since 2.3
     */
    protected boolean _hasTextualNull(String value) {
        return "null".equals(value);
    }

    protected final boolean _isNegInf(String text) {
        return "-Infinity".equals(text) || "-INF".equals(text);
    }

    protected final boolean _isPosInf(String text) {
        return "Infinity".equals(text) || "INF".equals(text);
    }

    protected final boolean _isNaN(String text) { return "NaN".equals(text); }

    // @since 2.12
    protected final static boolean _isBlank(String text)
    {
        final int len = text.length();
        for (int i = 0; i < len; ++i) {
            if (text.charAt(i) > 0x0020) {
                return false;
            }
        }
        return true;
    }

    /*
    /****************************************************
    /* Helper methods for sub-classes, new (2.12+)
    /****************************************************
     */

    /**
     * @since 2.12
     */
    protected CoercionAction _checkFromStringCoercion(DeserializationContext ctxt, String value)
        throws IOException
    {
        return _checkFromStringCoercion(ctxt, value, logicalType(), handledType());
    }

    /**
     * @since 2.12
     */
    protected CoercionAction _checkFromStringCoercion(DeserializationContext ctxt, String value,
            LogicalType logicalType, Class<?> rawTargetType)
        throws IOException
    {
        // 18-Dec-2020, tatu: Formats without strong typing (XML, CSV, Properties at
        //    least) should allow from-String "coercion" since Strings are their
        //    native type.
        //    One open question is whether Empty/Blank String are special; they might
        //    be so only apply short-cut to other cases, for now

        final CoercionAction act;
        if (value.isEmpty()) {
            act = ctxt.findCoercionAction(logicalType, rawTargetType,
                    CoercionInputShape.EmptyString);
            return _checkCoercionFail(ctxt, act, rawTargetType, value,
                    "empty String (\"\")");
        } else if (_isBlank(value)) {
            act = ctxt.findCoercionFromBlankString(logicalType, rawTargetType, CoercionAction.Fail);
            return _checkCoercionFail(ctxt, act, rawTargetType, value,
                    "blank String (all whitespace)");
        } else {
            // 18-Dec-2020, tatu: As per above, allow for XML, CSV, Properties
            if (ctxt.isEnabled(StreamReadCapability.UNTYPED_SCALARS)) {
                return CoercionAction.TryConvert;
            }
            act = ctxt.findCoercionAction(logicalType, rawTargetType, CoercionInputShape.String);
            if (act == CoercionAction.Fail) {
                // since it MIGHT (but might not), create desc here, do not use helper
                ctxt.reportInputMismatch(this,
"Cannot coerce String value (\"%s\") to %s (but might if coercion using `CoercionConfig` was enabled)",
value, _coercedTypeDesc());
            }
        }
        return act;
    }

    /**
     * @since 2.12
     */
    protected CoercionAction _checkFloatToIntCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        final CoercionAction act = ctxt.findCoercionAction(LogicalType.Integer,
                rawTargetType, CoercionInputShape.Float);
        if (act == CoercionAction.Fail) {
            return _checkCoercionFail(ctxt, act, rawTargetType, p.getNumberValue(),
                    "Floating-point value ("+p.getText()+")");
        }
        return act;
    }

    /**
     * @since 2.14
     */
    protected CoercionAction _checkIntToStringCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        return _checkToStringCoercion(p, ctxt, rawTargetType, p.getNumberValue(), CoercionInputShape.Integer);
    }

    /**
     * @since 2.14
     */
    protected CoercionAction _checkFloatToStringCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        return _checkToStringCoercion(p, ctxt, rawTargetType, p.getNumberValue(), CoercionInputShape.Float);
    }

    /**
     * @since 2.14
     */
    protected CoercionAction _checkBooleanToStringCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        return _checkToStringCoercion(p, ctxt, rawTargetType, p.getBooleanValue(), CoercionInputShape.Boolean);
    }

    /**
     * @since 2.14
     */
    protected CoercionAction _checkToStringCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType, Object inputValue, CoercionInputShape inputShape)
        throws IOException
    {
        final CoercionAction act = ctxt.findCoercionAction(LogicalType.Textual,
                rawTargetType, inputShape);
        if (act == CoercionAction.Fail) {
            return _checkCoercionFail(ctxt, act, rawTargetType, inputValue,
                    inputShape.name() + " value (" + p.getText() + ")");
        }
        return act;
    }

    /**
     * @since 2.14
     */
    protected CoercionAction _checkIntToFloatCoercion(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        final CoercionAction act = ctxt.findCoercionAction(LogicalType.Float,
                rawTargetType, CoercionInputShape.Integer);
        if (act == CoercionAction.Fail) {
            return _checkCoercionFail(ctxt, act, rawTargetType, p.getNumberValue(),
                    "Integer value (" + p.getText() + ")");
        }
        return act;
    }

    /**
     * @since 2.12
     */
    protected Boolean _coerceBooleanFromInt(JsonParser p, DeserializationContext ctxt,
            Class<?> rawTargetType)
        throws IOException
    {
        CoercionAction act = ctxt.findCoercionAction(LogicalType.Boolean, rawTargetType, CoercionInputShape.Integer);
        switch (act) {
        case Fail:
            _checkCoercionFail(ctxt, act, rawTargetType, p.getNumberValue(),
                    "Integer value ("+p.getText()+")");
            return Boolean.FALSE;
        case AsNull:
            return null;
        case AsEmpty:
            return Boolean.FALSE;
        default:
        }
        // 13-Oct-2016, tatu: As per [databind#1324], need to be careful wrt
        //    degenerate case of huge integers, legal in JSON.
        //    Also note that number tokens can not have WS to trim:
        if (p.getNumberType() == NumberType.INT) {
            // but minor optimization for common case is possible:
            return p.getIntValue() != 0;
        }
        return !"0".equals(p.getText());
    }

    /**
     * @since 2.12
     */
    protected CoercionAction _checkCoercionFail(DeserializationContext ctxt,
            CoercionAction act, Class<?> targetType, Object inputValue,
            String inputDesc)
        throws IOException
    {
        if (act == CoercionAction.Fail) {
            ctxt.reportBadCoercion(this, targetType, inputValue,
"Cannot coerce %s to %s (but could if coercion was enabled using `CoercionConfig`)",
inputDesc, _coercedTypeDesc(targetType));
        }
        return act;
    }

    /**
     * Method called when otherwise unrecognized String value is encountered for
     * a non-primitive type: should see if it is String value {@code "null"}, and if so,
     * whether it is acceptable according to configuration or not
     *
     * @since 2.12
     */
    protected boolean _checkTextualNull(DeserializationContext ctxt, String text)
            throws JsonMappingException
    {
        if (_hasTextualNull(text)) {
            if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
                _reportFailedNullCoerce(ctxt, true,  MapperFeature.ALLOW_COERCION_OF_SCALARS, "String \"null\"");
            }
            return true;
        }
        return false;
    }

    /*
    /**********************************************************************
    /* Helper methods for sub-classes, coercions, older (pre-2.12), non-deprecated
    /**********************************************************************
     */

    /**
     * Helper method called in case where an integral number is encountered, but
     * config settings suggest that a coercion may be needed to "upgrade"
     * {@link java.lang.Number} into "bigger" type like {@link java.lang.Long} or
     * {@link java.math.BigInteger}
     *
     * @see DeserializationFeature#USE_BIG_INTEGER_FOR_INTS
     * @see DeserializationFeature#USE_LONG_FOR_INTS
     *
     * @since 2.6
     */
    protected Object _coerceIntegral(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
            return p.getBigIntegerValue();
        }
        if (ctxt.isEnabled(DeserializationFeature.USE_LONG_FOR_INTS)) {
            return p.getLongValue();
        }
        return p.getNumberValue(); // should be optimal, whatever it is
    }

    /**
     * Method called to verify that {@code null} token from input is acceptable
     * for primitive (unboxed) target type. It should NOT be called if {@code null}
     * was received by other means (coerced due to configuration, or even from
     * optionally acceptable String {@code "null"} token).
     *
     * @since 2.9
     */
    protected final void _verifyNullForPrimitive(DeserializationContext ctxt) throws JsonMappingException
    {
        if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
            ctxt.reportInputMismatch(this,
"Cannot coerce `null` to %s (disable `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` to allow)",
                    _coercedTypeDesc());
        }
    }

    /**
     * Method called to verify that text value {@code "null"} from input is acceptable
     * for primitive (unboxed) target type. It should not be called if actual
     * {@code null} token was received, or if null is a result of coercion from
     * Some other input type.
     *
     * @since 2.9
     */
    protected final void _verifyNullForPrimitiveCoercion(DeserializationContext ctxt, String str) throws JsonMappingException
    {
        Enum<?> feat;
        boolean enable;

        if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
            feat = MapperFeature.ALLOW_COERCION_OF_SCALARS;
            enable = true;
        } else if (ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
            feat = DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES;
            enable = false;
        } else {
            return;
        }
        String strDesc = str.isEmpty() ? "empty String (\"\")" : String.format("String \"%s\"", str);
        _reportFailedNullCoerce(ctxt, enable, feat, strDesc);
    }

    protected void _reportFailedNullCoerce(DeserializationContext ctxt, boolean state, Enum<?> feature,
            String inputDesc) throws JsonMappingException
    {
        String enableDesc = state ? "enable" : "disable";
        ctxt.reportInputMismatch(this, "Cannot coerce %s to Null value as %s (%s `%s.%s` to allow)",
            inputDesc, _coercedTypeDesc(), enableDesc, feature.getDeclaringClass().getSimpleName(), feature.name());
    }

    /**
     * Helper method called to get a description of type into which a scalar value coercion
     * is (most likely) being applied, to be used for constructing exception messages
     * on coerce failure.
     *
     * @return Message with backtick-enclosed name of type this deserializer supports
     *
     * @since 2.9
     */
    protected String _coercedTypeDesc() {
        boolean structured;
        String typeDesc;

        JavaType t = getValueType();
        if ((t != null) && !t.isPrimitive()) {
            structured = (t.isContainerType() || t.isReferenceType());
            typeDesc = ClassUtil.getTypeDescription(t);
        } else {
            Class<?> cls = handledType();
            structured = ClassUtil.isCollectionMapOrArray(cls);
            typeDesc = ClassUtil.getClassDescription(cls);
        }
        if (structured) {
            return "element of "+typeDesc;
        }
        return typeDesc+" value";
    }

    /**
     * Helper method called to get a description of type into which a scalar value coercion
     * is being applied, to be used for constructing exception messages
     * on coerce failure.
     *
     * @return Message with backtick-enclosed name of target type
     *
     * @since 2.15
     */
    protected String _coercedTypeDesc(Class<?> rawTargetType) {
        String typeDesc = ClassUtil.getClassDescription(rawTargetType);
        if (ClassUtil.isCollectionMapOrArray(rawTargetType)) {
            return "element of "+typeDesc;
        }
        return typeDesc+" value";
    }

    /*
    /**********************************************************************
    /* Helper methods for sub-classes, coercions, older (pre-2.12), deprecated
    /**********************************************************************
     */

    @Deprecated // since 2.12
    protected boolean _parseBooleanFromInt(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // 13-Oct-2016, tatu: As per [databind#1324], need to be careful wrt
        //    degenerate case of huge integers, legal in JSON.
        //  ... this is, on the other hand, probably wrong/sub-optimal for non-JSON
        //  input. For now, no rea
        _verifyNumberForScalarCoercion(ctxt, p);
        // Anyway, note that since we know it's valid (JSON) integer, it can't have
        // extra whitespace to trim.
        return !"0".equals(p.getText());
    }

    /**
     * @deprecated Since 2.12 use {@link #_checkFromStringCoercion} instead
     */
    @Deprecated
    protected void _verifyStringForScalarCoercion(DeserializationContext ctxt, String str) throws JsonMappingException
    {
        MapperFeature feat = MapperFeature.ALLOW_COERCION_OF_SCALARS;
        if (!ctxt.isEnabled(feat)) {
            ctxt.reportInputMismatch(this, "Cannot coerce String \"%s\" to %s (enable `%s.%s` to allow)",
                str, _coercedTypeDesc(), feat.getDeclaringClass().getSimpleName(), feat.name());
        }
    }

    /**
     * Method called when JSON String with value "" (that is, zero length) is encountered.
     *
     * @deprecated Since 2.12
     */
    @Deprecated
    protected Object _coerceEmptyString(DeserializationContext ctxt, boolean isPrimitive) throws JsonMappingException
    {
        Enum<?> feat;
        boolean enable;

        if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
            feat = MapperFeature.ALLOW_COERCION_OF_SCALARS;
            enable = true;
        } else if (isPrimitive && ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
            feat = DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES;
            enable = false;
        } else {
            return getNullValue(ctxt);
        }
        _reportFailedNullCoerce(ctxt, enable, feat, "empty String (\"\")");
        return null;
    }

    @Deprecated // since 2.12
    protected void _failDoubleToIntCoercion(JsonParser p, DeserializationContext ctxt,
            String type) throws IOException
    {
        ctxt.reportInputMismatch(handledType(),
"Cannot coerce a floating-point value ('%s') into %s (enable `DeserializationFeature.ACCEPT_FLOAT_AS_INT` to allow)",
                p.getValueAsString(), type);
    }

    @Deprecated // since 2.12
    protected final void _verifyNullForScalarCoercion(DeserializationContext ctxt, String str) throws JsonMappingException
    {
        if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
            String strDesc = str.isEmpty() ? "empty String (\"\")" : String.format("String \"%s\"", str);
            _reportFailedNullCoerce(ctxt, true, MapperFeature.ALLOW_COERCION_OF_SCALARS, strDesc);
        }
    }

    @Deprecated // since 2.12
    protected void _verifyNumberForScalarCoercion(DeserializationContext ctxt, JsonParser p) throws IOException
    {
        MapperFeature feat = MapperFeature.ALLOW_COERCION_OF_SCALARS;
        if (!ctxt.isEnabled(feat)) {
            // 31-Mar-2017, tatu: Since we don't know (or this deep, care) about exact type,
            //   access as a String: may require re-encoding by parser which should be fine
            String valueDesc = p.getText();
            ctxt.reportInputMismatch(this, "Cannot coerce Number (%s) to %s (enable `%s.%s` to allow)",
                valueDesc, _coercedTypeDesc(), feat.getDeclaringClass().getSimpleName(), feat.name());
        }
    }

    @Deprecated // since 2.12
    protected Object _coerceNullToken(DeserializationContext ctxt, boolean isPrimitive) throws JsonMappingException
    {
        if (isPrimitive) {
            _verifyNullForPrimitive(ctxt);
        }
        return getNullValue(ctxt);
    }

    @Deprecated // since 2.12
    protected Object _coerceTextualNull(DeserializationContext ctxt, boolean isPrimitive) throws JsonMappingException {
        if (!ctxt.isEnabled(MapperFeature.ALLOW_COERCION_OF_SCALARS)) {
            _reportFailedNullCoerce(ctxt, true,  MapperFeature.ALLOW_COERCION_OF_SCALARS, "String \"null\"");
        }
        return getNullValue(ctxt);
    }

    @Deprecated // since 2.12
    protected boolean _isEmptyOrTextualNull(String value) {
        return value.isEmpty() || "null".equals(value);
    }

    /*
    /****************************************************
    /* Helper methods for sub-classes, resolving dependencies
    /****************************************************
     */

    /**
     * Helper method used to locate deserializers for properties the
     * type this deserializer handles contains (usually for properties of
     * bean types)
     *
     * @param type Type of property to deserialize
     * @param property Actual property object (field, method, constuctor parameter) used
     *     for passing deserialized values; provided so deserializer can be contextualized if necessary
     */
    protected JsonDeserializer<Object> findDeserializer(DeserializationContext ctxt,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        return ctxt.findContextualValueDeserializer(type, property);
    }

    /**
     * Helper method to check whether given text refers to what looks like a clean simple
     * integer number, consisting of optional sign followed by a sequence of digits.
     *<p>
     * Note that definition is quite loose as leading zeroes are allowed, in addition
     * to plus sign (not just minus).
     */
    protected final boolean _isIntNumber(String text)
    {
        final int len = text.length();
        if (len > 0) {
            char c = text.charAt(0);
            // skip leading sign (plus not allowed for strict JSON numbers but...)
            int i;

            if (c == '-' || c == '+') {
                if (len == 1) {
                    return false;
                }
                i = 1;
            } else {
                i = 0;
            }
            // We will allow leading
            for (; i < len; ++i) {
                int ch = text.charAt(i);
                if (ch > '9' || ch < '0') {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*
    /**********************************************************
    /* Helper methods for: deserializer construction
    /**********************************************************
     */

    /**
     * Helper method that can be used to see if specified property has annotation
     * indicating that a converter is to be used for contained values (contents
     * of structured types; array/List/Map values)
     *
     * @param existingDeserializer (optional) configured content
     *    serializer if one already exists.
     *
     * @since 2.2
     */
    protected JsonDeserializer<?> findConvertingContentDeserializer(DeserializationContext ctxt,
            BeanProperty prop, JsonDeserializer<?> existingDeserializer)
        throws JsonMappingException
    {
        final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
        if (_neitherNull(intr, prop)) {
            AnnotatedMember member = prop.getMember();
            if (member != null) {
                Object convDef = intr.findDeserializationContentConverter(member);
                if (convDef != null) {
                    Converter<Object,Object> conv = ctxt.converterInstance(prop.getMember(), convDef);
                    JavaType delegateType = conv.getInputType(ctxt.getTypeFactory());
                    if (existingDeserializer == null) {
                        existingDeserializer = ctxt.findContextualValueDeserializer(delegateType, prop);
                    }
                    return new StdDelegatingDeserializer<Object>(conv, delegateType, existingDeserializer);
                }
            }
        }
        return existingDeserializer;
    }

    /*
    /**********************************************************
    /* Helper methods for: accessing contextual config settings
    /**********************************************************
     */

    /**
     * Helper method that may be used to find if this deserializer has specific
     * {@link JsonFormat} settings, either via property, or through type-specific
     * defaulting.
     *
     * @param typeForDefaults Type (erased) used for finding default format settings, if any
     *
     * @since 2.7
     */
    protected JsonFormat.Value findFormatOverrides(DeserializationContext ctxt,
            BeanProperty prop, Class<?> typeForDefaults)
    {
        if (prop != null) {
            return prop.findPropertyFormat(ctxt.getConfig(), typeForDefaults);
        }
        // even without property or AnnotationIntrospector, may have type-specific defaults
        return ctxt.getDefaultPropertyFormat(typeForDefaults);
    }

    /**
     * Convenience method that uses {@link #findFormatOverrides} to find possible
     * defaults and/of overrides, and then calls
     * <code>JsonFormat.Value.getFeature(feat)</code>
     * to find whether that feature has been specifically marked as enabled or disabled.
     *
     * @param typeForDefaults Type (erased) used for finding default format settings, if any
     *
     * @since 2.7
     */
    protected Boolean findFormatFeature(DeserializationContext ctxt,
            BeanProperty prop, Class<?> typeForDefaults, JsonFormat.Feature feat)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt, prop, typeForDefaults);
        if (format != null) {
            return format.getFeature(feat);
        }
        return null;
    }

    /**
     * Method called to find {@link NullValueProvider} for a primary property, using
     * "value nulls" setting. If no provider found (not defined, or is "skip"),
     * will return `null`.
     *
     * @since 2.9
     */
    protected final NullValueProvider findValueNullProvider(DeserializationContext ctxt,
            SettableBeanProperty prop, PropertyMetadata propMetadata)
        throws JsonMappingException
    {
        if (prop != null) {
            return _findNullProvider(ctxt, prop, propMetadata.getValueNulls(),
                    prop.getValueDeserializer());
        }
        return null;
    }

    /**
     * Method called to find {@link NullValueProvider} for a contents of a structured
     * primary property (Collection, Map, array), using
     * "content nulls" setting. If no provider found (not defined),
     * will return given value deserializer (which is a null value provider itself).
     *
     * @since 2.9
     */
    protected NullValueProvider findContentNullProvider(DeserializationContext ctxt,
            BeanProperty prop, JsonDeserializer<?> valueDeser)
        throws JsonMappingException
    {
        final Nulls nulls = findContentNullStyle(ctxt, prop);
        if (nulls == Nulls.SKIP) {
            return NullsConstantProvider.skipper();
        }
        // 09-Dec-2019, tatu: [databind#2567] need to ensure correct target type (element,
        //    not container), so inlined here before calling _findNullProvider
        if (nulls == Nulls.FAIL) {
            if (prop == null) {
                JavaType type = ctxt.constructType(valueDeser.handledType());
                // should always be container? But let's double-check just in case:
                if (type.isContainerType()) {
                    type = type.getContentType();
                }
                return NullsFailProvider.constructForRootValue(type);
            }
            return NullsFailProvider.constructForProperty(prop, prop.getType().getContentType());
        }

        NullValueProvider prov = _findNullProvider(ctxt, prop, nulls, valueDeser);
        if (prov != null) {
            return prov;
        }
        return valueDeser;
    }

    protected Nulls findContentNullStyle(DeserializationContext ctxt, BeanProperty prop)
        throws JsonMappingException
    {
        if (prop != null) {
            return prop.getMetadata().getContentNulls();
        }
        // 24-Aug-2021, tatu: As per [databind#3227] root values also need
        //     to be checked for content nulls
        //  NOTE: will this work wrt type-specific overrides? Probably not...
        return ctxt.getConfig().getDefaultSetterInfo().getContentNulls();
    }

    // @since 2.9
    protected final NullValueProvider _findNullProvider(DeserializationContext ctxt,
            BeanProperty prop, Nulls nulls, JsonDeserializer<?> valueDeser)
        throws JsonMappingException
    {
        if (nulls == Nulls.FAIL) {
            if (prop == null) {
                Class<?> rawType = (valueDeser == null) ? Object.class : valueDeser.handledType();
                return NullsFailProvider.constructForRootValue(ctxt.constructType(rawType));
            }
            return NullsFailProvider.constructForProperty(prop);
        }
        if (nulls == Nulls.AS_EMPTY) {
            // cannot deal with empty values if there is no value deserializer that
            // can indicate what "empty value" is:
            if (valueDeser == null) {
                return null;
            }

            // Let's first do some sanity checking...
            // NOTE: although we could use `ValueInstantiator.Gettable` in general,
            // let's not since that would prevent being able to use custom impls:
            if (valueDeser instanceof BeanDeserializerBase) {
                BeanDeserializerBase bd = (BeanDeserializerBase) valueDeser;
                ValueInstantiator vi = bd.getValueInstantiator();
                if (!vi.canCreateUsingDefault()) {
                    final JavaType type = (prop == null) ? bd.getValueType() : prop.getType();
                    return ctxt.reportBadDefinition(type,
                            String.format("Cannot create empty instance of %s, no default Creator", type));
                }
            }
            // Second: can with pre-fetch value?
            {
                AccessPattern access = valueDeser.getEmptyAccessPattern();
                if (access == AccessPattern.ALWAYS_NULL) {
                    return NullsConstantProvider.nuller();
                }
                if (access == AccessPattern.CONSTANT) {
                    return NullsConstantProvider.forValue(valueDeser.getEmptyValue(ctxt));
                }
            }
            return new NullsAsEmptyProvider(valueDeser);
        }
        if (nulls == Nulls.SKIP) {
            return NullsConstantProvider.skipper();
        }
        return null;
    }

    // @since 2.12
    protected CoercionAction _findCoercionFromEmptyString(DeserializationContext ctxt) {
        return ctxt.findCoercionAction(logicalType(), handledType(),
                CoercionInputShape.EmptyString);
    }

    // @since 2.12
    protected CoercionAction _findCoercionFromEmptyArray(DeserializationContext ctxt) {
        return ctxt.findCoercionAction(logicalType(), handledType(),
                CoercionInputShape.EmptyArray);
    }

    // @since 2.12
    protected CoercionAction _findCoercionFromBlankString(DeserializationContext ctxt) {
        return ctxt.findCoercionFromBlankString(logicalType(), handledType(),
                CoercionAction.Fail);
    }

    /*
    /**********************************************************
    /* Helper methods for sub-classes, problem reporting
    /**********************************************************
     */

    /**
     * Method called to deal with a property that did not map to a known
     * Bean property. Method can deal with the problem as it sees fit (ignore,
     * throw exception); but if it does return, it has to skip the matching
     * Json content parser has.
     *
     * @param p Parser that points to value of the unknown property
     * @param ctxt Context for deserialization; allows access to the parser,
     *    error reporting functionality
     * @param instanceOrClass Instance that is being populated by this
     *   deserializer, or if not known, Class that would be instantiated.
     *   If null, will assume type is what {@link #getValueClass} returns.
     * @param propName Name of the property that cannot be mapped
     */
    protected void handleUnknownProperty(JsonParser p, DeserializationContext ctxt,
            Object instanceOrClass, String propName)
        throws IOException
    {
        if (instanceOrClass == null) {
            instanceOrClass = handledType();
        }
        // Maybe we have configured handler(s) to take care of it?
        if (ctxt.handleUnknownProperty(p, this, instanceOrClass, propName)) {
            return;
        }
        /* But if we do get this far, need to skip whatever value we
         * are pointing to now (although handler is likely to have done that already)
         */
        p.skipChildren();
    }

    protected void handleMissingEndArrayForSingle(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        ctxt.reportWrongTokenException(this, JsonToken.END_ARRAY,
"Attempted to unwrap '%s' value from an array (with `DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS`) but it contains more than one value",
handledType().getName());
        // 05-May-2016, tatu: Should recover somehow (maybe skip until END_ARRAY);
        //     but for now just fall through
    }

    /**
     * Helper method called when detecting a deep(er) nesting of Arrays when trying
     * to unwrap value for {@code DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS}.
     *
     * @since 2.14
     */
    protected Object handleNestedArrayForSingle(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        String msg = String.format(
"Cannot deserialize instance of %s out of %s token: nested Arrays not allowed with %s",
                ClassUtil.nameOf(_valueClass), JsonToken.START_ARRAY,
                "DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS");
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p.currentToken(), p, msg);
    }

    protected void _verifyEndArrayForSingle(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.nextToken();
        if (t != JsonToken.END_ARRAY) {
            handleMissingEndArrayForSingle(p, ctxt);
        }
    }

    /*
    /**********************************************************
    /* Helper methods, other
    /**********************************************************
     */

    /**
     * @since 2.9
     */
    protected final static boolean _neitherNull(Object a, Object b) {
        return (a != null) && (b != null);
    }

    /**
     * @since 2.9
     */
    protected final boolean _byteOverflow(int value) {
        // 07-nov-2016, tatu: We support "unsigned byte" as well
        //    as Java signed range since that's relatively common usage
        return (value < Byte.MIN_VALUE || value > 255);
    }

    /**
     * @since 2.9
     */
    protected final boolean _shortOverflow(int value) {
        return (value < Short.MIN_VALUE || value > Short.MAX_VALUE);
    }

    /**
     * @since 2.9
     */
    protected final boolean _intOverflow(long value) {
        return (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE);
    }

    /**
     * @since 2.9
     */
    protected Number _nonNullNumber(Number n) {
        if (n == null) {
            n = Integer.valueOf(0);
        }
        return n;
    }
}

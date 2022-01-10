package com.fasterxml.jackson.databind.deser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.deser.std.NullifyingDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Container class for deserializers that handle core JDK primitive
 * (and matching wrapper) types, as well as standard "big" numeric types.
 * Note that this includes types such as {@link java.lang.Boolean}
 * and {@link java.lang.Character} which are not strictly numeric,
 * but are part of primitive/wrapper types.
 */
public class NumberDeserializers
{
    private final static HashSet<String> _classNames = new HashSet<String>();
    static {
        // note: can skip primitive types; other ways to check them:
        Class<?>[] numberTypes = new Class<?>[] {
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            // and more generic ones
            Number.class, BigDecimal.class, BigInteger.class
        };
        for (Class<?> cls : numberTypes) {
            _classNames.add(cls.getName());
        }
    }

    public static ValueDeserializer<?> find(Class<?> rawType) {
        if (rawType.isPrimitive()) {
            if (rawType == Integer.TYPE) {
                return IntegerDeserializer.primitiveInstance;
            }
            if (rawType == Boolean.TYPE) {
                return BooleanDeserializer.primitiveInstance;
            }
            if (rawType == Long.TYPE) {
                return LongDeserializer.primitiveInstance;
            }
            if (rawType == Double.TYPE) {
                return DoubleDeserializer.primitiveInstance;
            }
            if (rawType == Character.TYPE) {
                return CharacterDeserializer.primitiveInstance;
            }
            if (rawType == Byte.TYPE) {
                return ByteDeserializer.primitiveInstance;
            }
            if (rawType == Short.TYPE) {
                return ShortDeserializer.primitiveInstance;
            }
            if (rawType == Float.TYPE) {
                return FloatDeserializer.primitiveInstance;
            }
            // [databind#2679]: bit odd place for this (Void.class handled in
            // `JdkDeserializers`), due to `void` being primitive type
            if (rawType == Void.TYPE) {
                return NullifyingDeserializer.instance;
            }
        } else if (_classNames.contains(rawType.getName())) {
            // Start with most common types; int, boolean, long, double
            if (rawType == Integer.class) {
                return IntegerDeserializer.wrapperInstance;
            }
            if (rawType == Boolean.class) {
                return BooleanDeserializer.wrapperInstance;
            }
            if (rawType == Long.class) {
                return LongDeserializer.wrapperInstance;
            }
            if (rawType == Double.class) {
                return DoubleDeserializer.wrapperInstance;
            }
            if (rawType == Character.class) {
                return CharacterDeserializer.wrapperInstance;
            }
            if (rawType == Byte.class) {
                return ByteDeserializer.wrapperInstance;
            }
            if (rawType == Short.class) {
                return ShortDeserializer.wrapperInstance;
            }
            if (rawType == Float.class) {
                return FloatDeserializer.wrapperInstance;
            }
            if (rawType == Number.class) {
                return NumberDeserializer.instance;
            }
            if (rawType == BigDecimal.class) {
                return BigDecimalDeserializer.instance;
            }
            if (rawType == BigInteger.class) {
                return BigIntegerDeserializer.instance;
            }
        } else {
            return null;
        }
        // should never occur
        throw new IllegalArgumentException("Internal error: can't find deserializer for "+rawType.getName());
    }
    
    /*
    /**********************************************************
    /* Then one intermediate base class for things that have
    /* both primitive and wrapper types
    /**********************************************************
     */

    protected abstract static class PrimitiveOrWrapperDeserializer<T>
        extends StdScalarDeserializer<T>
    {
        protected final LogicalType _logicalType;

        protected final T _nullValue;

        protected final T _emptyValue;

        protected final boolean _primitive;

        protected PrimitiveOrWrapperDeserializer(Class<T> vc, LogicalType logicalType,
                T nvl, T empty) {
            super(vc);
            _logicalType = logicalType;
            _nullValue = nvl;
            _emptyValue = empty;
            _primitive = vc.isPrimitive();
        }

        @Override
        public AccessPattern getNullAccessPattern() {
            // 02-Feb-2017, tatu: For primitives we must dynamically check (and possibly throw
            //     exception); for wrappers not.
            if (_primitive) {
                return AccessPattern.DYNAMIC;
            }
            if (_nullValue == null) {
                return AccessPattern.ALWAYS_NULL;
            }
            return AccessPattern.CONSTANT;
        }

        @Override
        public final T getNullValue(DeserializationContext ctxt) {
            // 01-Mar-2017, tatu: Alas, not all paths lead to `_coerceNull()`, as `SettableBeanProperty`
            //    short-circuits `null` handling. Hence need this check as well.
            if (_primitive && ctxt.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)) {
                ctxt.reportInputMismatch(this,
                        "Cannot map `null` into type %s (set DeserializationConfig.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES to 'false' to allow)",
                        ClassUtil.classNameOf(handledType()));
            }
            return _nullValue;
        }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return _emptyValue;
        }

        @Override
        public LogicalType logicalType() {
            return _logicalType;
        }
    }

    /*
    /**********************************************************
    /* Then primitive/wrapper types
    /**********************************************************
     */

    @JacksonStdImpl
    public final static class BooleanDeserializer
        extends PrimitiveOrWrapperDeserializer<Boolean>
    {
        final static BooleanDeserializer primitiveInstance = new BooleanDeserializer(Boolean.TYPE, Boolean.FALSE);
        final static BooleanDeserializer wrapperInstance = new BooleanDeserializer(Boolean.class, null);

        public BooleanDeserializer(Class<Boolean> cls, Boolean nvl)
        {
            super(cls, LogicalType.Boolean, nvl, Boolean.FALSE);
        }

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (_primitive) {
                return _parseBooleanPrimitive(p, ctxt);
            }
            return _parseBoolean(p, ctxt, _valueClass);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Boolean deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws JacksonException
        {
            JsonToken t = p.currentToken();
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (_primitive) {
                return _parseBooleanPrimitive(p, ctxt);
            }
            return _parseBoolean(p, ctxt, _valueClass);
        }
    }

    @JacksonStdImpl
    public static class ByteDeserializer
        extends PrimitiveOrWrapperDeserializer<Byte>
    {
        final static ByteDeserializer primitiveInstance = new ByteDeserializer(Byte.TYPE, (byte) 0);
        final static ByteDeserializer wrapperInstance = new ByteDeserializer(Byte.class, null);

        public ByteDeserializer(Class<Byte> cls, Byte nvl)
        {
            super(cls, LogicalType.Integer, nvl, (byte) 0);
        }

        @Override
        public Byte deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            if (p.isExpectedNumberIntToken()) {
                return p.getByteValue();
            }
            if (_primitive) {
                return _parseBytePrimitive(p, ctxt);
            }
            return _parseByte(p, ctxt);
        }

        protected Byte _parseByte(JsonParser p, DeserializationContext ctxt)
                throws JacksonException
        {
            String text;

            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                text = p.getText();
                break;
            case JsonTokenId.ID_NUMBER_FLOAT:
                final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Byte) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Byte) getEmptyValue(ctxt);
                }
                return p.getByteValue();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Byte) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_INT:
                return p.getByteValue();
            case JsonTokenId.ID_START_ARRAY:
                return (Byte) _deserializeFromArray(p, ctxt);
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            default:
                return (Byte) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            // Rest of the processing is for coercion from String
            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (Byte) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Byte) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (Byte) getNullValue(ctxt);
            }
            int value;
            try {
                value = NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Byte value");
            }
            // So far so good: but does it fit?
            // as per [JACKSON-804], allow range up to 255, inclusive
            if (_byteOverflow(value)) {
                return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                        "overflow, value cannot be represented as 8-bit value");
                // fall-through for deferred fails
            }
            return Byte.valueOf((byte) value);
        }
    }

    @JacksonStdImpl
    public static class ShortDeserializer
        extends PrimitiveOrWrapperDeserializer<Short>
    {
        final static ShortDeserializer primitiveInstance = new ShortDeserializer(Short.TYPE, Short.valueOf((short)0));
        final static ShortDeserializer wrapperInstance = new ShortDeserializer(Short.class, null);
        
        public ShortDeserializer(Class<Short> cls, Short nvl)
        {
            super(cls, LogicalType.Integer, nvl, (short)0);
        }

        @Override
        public Short deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            if (p.isExpectedNumberIntToken()) {
                return p.getShortValue();
            }
            if (_primitive) {
                return _parseShortPrimitive(p, ctxt);
            }
            return _parseShort(p, ctxt);
        }

        protected Short _parseShort(JsonParser p, DeserializationContext ctxt)
                throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                text = p.getText();
                break;
            case JsonTokenId.ID_NUMBER_FLOAT:
                final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (Short) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (Short) getEmptyValue(ctxt);
                }
                return p.getShortValue();
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Short) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_INT:
                return p.getShortValue();
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return (Short)_deserializeFromArray(p, ctxt);
            default:
                return (Short) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            // Rest of the processing is for coercion from String
            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (Short) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Short) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (Short) getNullValue(ctxt);
            }
            int value;
            try {
                value = NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Short value");
            }
            // So far so good: but does it fit?
            if (_shortOverflow(value)) {
                return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                        "overflow, value cannot be represented as 16-bit value");
            }
            return (short) value;
        }
    }

    @JacksonStdImpl
    public static class CharacterDeserializer
        extends PrimitiveOrWrapperDeserializer<Character>
    {
        final static CharacterDeserializer primitiveInstance = new CharacterDeserializer(Character.TYPE, '\0');
        final static CharacterDeserializer wrapperInstance = new CharacterDeserializer(Character.class, null);
        
        public CharacterDeserializer(Class<Character> cls, Character nvl)
        {
            super(cls,
                    // 07-Jun-2020, tatu: Debatable if it should be "OtherScalar" or Integer but...
                    LogicalType.Integer, nvl, '\0');
        }

        @Override
        public Character deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                // 23-Jun-2020, tatu: Unlike real numeric types, Character/char does not
                //   have canonical shape in JSON, and String in particular does not need
                //   coercion -- as long as it has length of 1.
                text = p.getText();
                break;
            case JsonTokenId.ID_NUMBER_INT: // ok iff Unicode value
                CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass, CoercionInputShape.Integer);
                switch (act) {
                case Fail:
                    _checkCoercionFail(ctxt, act, _valueClass, p.getNumberValue(),
                            "Integer value ("+p.getText()+")");
                    // fall-through in unlikely case of returning
                case AsNull:
                    return getNullValue(ctxt);
                case AsEmpty:
                    return (Character) getEmptyValue(ctxt);
                default:
                }
                int value = p.getIntValue();
                if (value >= 0 && value <= 0xFFFF) {
                    return Character.valueOf((char) value);
                }
                return (Character) ctxt.handleWeirdNumberValue(handledType(), value,
                        "value outside valid Character range (0x0000 - 0xFFFF)");
            case JsonTokenId.ID_NULL:
                if (_primitive) {
                    _verifyNullForPrimitive(ctxt);
                }
                return (Character) getNullValue(ctxt);
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                return (Character) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            if (text.length() == 1) {
                return Character.valueOf(text.charAt(0));
            }
            CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Character) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (Character) getNullValue(ctxt);
            }
            return (Character) ctxt.handleWeirdStringValue(handledType(), text,
                    "Expected either Integer value code or 1-character String");
        }
    }

    @JacksonStdImpl
    public final static class IntegerDeserializer
        extends PrimitiveOrWrapperDeserializer<Integer>
    {
        final static IntegerDeserializer primitiveInstance = new IntegerDeserializer(Integer.TYPE, 0);
        final static IntegerDeserializer wrapperInstance = new IntegerDeserializer(Integer.class, null);
        
        public IntegerDeserializer(Class<Integer> cls, Integer nvl) {
            super(cls, LogicalType.Integer, nvl, 0);
        }

        @Override
        public boolean isCachable() { return true; }

        @Override
        public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            if (p.isExpectedNumberIntToken()) {
                return p.getIntValue();
            }
            if (_primitive) {
                return _parseIntPrimitive(p, ctxt);
            }
            return _parseInteger(p, ctxt, Integer.class);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Integer deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer) throws JacksonException
        {
            if (p.isExpectedNumberIntToken()) {
                return p.getIntValue();
            }
            if (_primitive) {
                return _parseIntPrimitive(p, ctxt);
            }
            return _parseInteger(p, ctxt, Integer.class);
        }
    }

    @JacksonStdImpl
    public final static class LongDeserializer
        extends PrimitiveOrWrapperDeserializer<Long>
    {
        final static LongDeserializer primitiveInstance = new LongDeserializer(Long.TYPE, 0L);
        final static LongDeserializer wrapperInstance = new LongDeserializer(Long.class, null);
        
        public LongDeserializer(Class<Long> cls, Long nvl) {
            super(cls, LogicalType.Integer, nvl, 0L);
        }

        @Override
        public boolean isCachable() { return true; }
        
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            if (p.isExpectedNumberIntToken()) {
                return p.getLongValue();
            }
            if (_primitive) {
                return _parseLongPrimitive(p, ctxt);
            }
            return _parseLong(p, ctxt, Long.class);
        }
    }

    @JacksonStdImpl
    public static class FloatDeserializer
        extends PrimitiveOrWrapperDeserializer<Float>
    {
        final static FloatDeserializer primitiveInstance = new FloatDeserializer(Float.TYPE, 0.f);
        final static FloatDeserializer wrapperInstance = new FloatDeserializer(Float.class, null);
        
        public FloatDeserializer(Class<Float> cls, Float nvl) {
            super(cls, LogicalType.Float, nvl, 0.f);
        }

        @Override
        public Float deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getFloatValue();
            }
            if (_primitive) {
                return _parseFloatPrimitive(p, ctxt);
            }
            return _parseFloat(p, ctxt);
        }

        protected final Float _parseFloat(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                text = p.getText();
                break;
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Float) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_NUMBER_INT: // safe coercion
                return p.getFloatValue();
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                return (Float) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            // 18-Nov-2020, tatu: Special case, Not-a-Numbers as String need to be
            //     considered "native" representation as JSON does not allow as numbers,
            //     and hence not bound by coercion rules
            {
                Float nan = _checkFloatSpecialValue(text);
                if (nan != null) {
                    return nan;
                }
            }
            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (Float) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Float) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (Float) getNullValue(ctxt);
            }
            try {
                return Float.parseFloat(text);
            } catch (IllegalArgumentException iae) { }
            return (Float) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid `Float` value");
        }
    }

    @JacksonStdImpl
    public static class DoubleDeserializer
        extends PrimitiveOrWrapperDeserializer<Double>
    {
        final static DoubleDeserializer primitiveInstance = new DoubleDeserializer(Double.TYPE, 0.d);
        final static DoubleDeserializer wrapperInstance = new DoubleDeserializer(Double.class, null);
        
        public DoubleDeserializer(Class<Double> cls, Double nvl) {
            super(cls, LogicalType.Float, nvl, 0.d);
        }

        @Override
        public Double deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getDoubleValue();
            }
            if (_primitive) {
                return _parseDoublePrimitive(p, ctxt);
            }
            return _parseDouble(p, ctxt);
        }

        // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
        // (is it an error to even call this version?)
        @Override
        public Double deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer) throws JacksonException
        {
            if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return p.getDoubleValue();
            }
            if (_primitive) {
                return _parseDoublePrimitive(p, ctxt);
            }
            return _parseDouble(p, ctxt);
        }

        protected final Double _parseDouble(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                text = p.getText();
                break;
            case JsonTokenId.ID_NULL: // null fine for non-primitive
                return (Double) getNullValue(ctxt);
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_NUMBER_INT: // safe coercion
                return p.getDoubleValue();
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                return (Double) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            // 18-Nov-2020, tatu: Special case, Not-a-Numbers as String need to be
            //     considered "native" representation as JSON does not allow as numbers,
            //     and hence not bound by coercion rules
            {
                Double nan = this._checkDoubleSpecialValue(text);
                if (nan != null) {
                    return nan;
                }
            }

            // Coercion from String most complicated
            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (Double) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (Double) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (Double) getNullValue(ctxt);
            }
            try {
                return _parseDouble(text);
            } catch (IllegalArgumentException iae) { }
            return (Double) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid `Double` value");
        }
    }

    /**
     * For type <code>Number.class</code>, we can just rely on type
     * mappings that plain {@link JsonParser#getNumberValue} returns.
     *<p>
     * There is one additional complication: some numeric
     * types (specifically, int/Integer and double/Double) are "non-typed";
     * meaning that they will NEVER be output with type information.
     * But other numeric types may need such type information.
     * This is why {@link #deserializeWithType} must be overridden.
     */
    @JacksonStdImpl
    public static class NumberDeserializer
        extends StdScalarDeserializer<Object>
    {
        public final static NumberDeserializer instance = new NumberDeserializer();
        
        public NumberDeserializer() {
            super(Number.class);
        }

        @Override
        public final LogicalType logicalType() {
            // 07-Jun-2020, tatu: Hmmh... tricky choice. For now, use:
            return LogicalType.Integer;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING:
                text = p.getText();
                break;
            case JsonTokenId.ID_NUMBER_INT:
                if (ctxt.hasSomeOfFeatures(F_MASK_INT_COERCIONS)) {
                    return _coerceIntegral(p, ctxt);
                }
                return p.getNumberValue();

            case JsonTokenId.ID_NUMBER_FLOAT:
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                    // 10-Mar-2017, tatu: NaN and BigDecimal won't mix...
                    if (!p.isNaN()) {
                        return p.getDecimalValue();
                    }
                }
                return p.getNumberValue();
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            // Textual values are more difficult... not parsing itself, but figuring
            // out 'minimal' type to use 
            CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_hasTextualNull(text)) {
                // note: no need to call `coerce` as this is never primitive
                return getNullValue(ctxt);
            }
            if (_isPosInf(text)) {
                return Double.POSITIVE_INFINITY;
            }
            if (_isNegInf(text)) {
                return Double.NEGATIVE_INFINITY;
            }
            if (_isNaN(text)) {
                return Double.NaN;
            }
            try {
                if (!_isIntNumber(text)) {
                    if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                        return new BigDecimal(text);
                    }
                    return Double.valueOf(text);
                }
                if (ctxt.isEnabled(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)) {
                    return new BigInteger(text);
                }
                long value = Long.parseLong(text);
                if (!ctxt.isEnabled(DeserializationFeature.USE_LONG_FOR_INTS)) {
                    if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                        return Integer.valueOf((int) value);
                    }
                }
                return Long.valueOf(value);
            } catch (IllegalArgumentException iae) {
                return ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid number");
            }
        }

        /**
         * As mentioned in class Javadoc, there is additional complexity in
         * handling potentially mixed type information here. Because of this,
         * we must actually check for "raw" integers and doubles first, before
         * calling type deserializer.
         */
        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws JacksonException
        {
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
            case JsonTokenId.ID_NUMBER_FLOAT:
            case JsonTokenId.ID_STRING:
                // cannot point to type information: hence must be non-typed (int/double)
                return deserialize(p, ctxt);
            }
            return typeDeserializer.deserializeTypedFromScalar(p, ctxt);
        }
    }

    /*
    /**********************************************************
    /* And then bit more complicated (but non-structured) number types
    /**********************************************************
     */

    /**
     * This is bit trickier to implement efficiently, while avoiding
     * overflow problems.
     */
    @JacksonStdImpl
    public static class BigIntegerDeserializer
        extends StdScalarDeserializer<BigInteger>
    {
        public final static BigIntegerDeserializer instance = new BigIntegerDeserializer();

        public BigIntegerDeserializer() { super(BigInteger.class); }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return BigInteger.ZERO;
        }

        @Override
        public final LogicalType logicalType() {
            return LogicalType.Integer;
        }

        @Override
        public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
        {
            if (p.isExpectedNumberIntToken()) {
                return p.getBigIntegerValue();
            }

            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_STRING: // let's do implicit re-parse
                text = p.getText();
                break;
            case JsonTokenId.ID_NUMBER_FLOAT:
                final CoercionAction act = _checkFloatToIntCoercion(p, ctxt, _valueClass);
                if (act == CoercionAction.AsNull) {
                    return (BigInteger) getNullValue(ctxt);
                }
                if (act == CoercionAction.AsEmpty) {
                    return (BigInteger) getEmptyValue(ctxt);
                }
                return p.getDecimalValue().toBigInteger();
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                // String is ok too, can easily convert; otherwise, no can do:
                return (BigInteger) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (BigInteger) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (BigInteger) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_hasTextualNull(text)) {
                // note: no need to call `coerce` as this is never primitive
                return (BigInteger) getNullValue(ctxt);
            }
            try {
                return new BigInteger(text);
            } catch (IllegalArgumentException iae) { }
            return (BigInteger) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid representation");
        }
    }

    @JacksonStdImpl
    public static class BigDecimalDeserializer
        extends StdScalarDeserializer<BigDecimal>
    {
        public final static BigDecimalDeserializer instance = new BigDecimalDeserializer();
 
        public BigDecimalDeserializer() { super(BigDecimal.class); }

        @Override
        public Object getEmptyValue(DeserializationContext ctxt) {
            return BigDecimal.ZERO;
        }

        @Override
        public final LogicalType logicalType() {
            return LogicalType.Float;
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            String text;
            switch (p.currentTokenId()) {
            case JsonTokenId.ID_NUMBER_INT:
            case JsonTokenId.ID_NUMBER_FLOAT:
                return p.getDecimalValue();
            case JsonTokenId.ID_STRING:
                text = p.getText();
                break;
            // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                text = ctxt.extractScalarFromObject(p, this, _valueClass);
                break;
            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            default:
                return (BigDecimal) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
            }

            final CoercionAction act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (BigDecimal) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (BigDecimal) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_hasTextualNull(text)) {
                // note: no need to call `coerce` as this is never primitive
                return (BigDecimal) getNullValue(ctxt);
            }
            try {
                return new BigDecimal(text);
            } catch (IllegalArgumentException iae) { }
            return (BigDecimal) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid representation");
        }
    }
}

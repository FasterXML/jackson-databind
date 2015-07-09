package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Base class for common deserializers. Contains shared
 * base functionality for dealing with primitive values, such
 * as (re)parsing from String.
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
    implements java.io.Serializable
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
    
    /**
     * Type of values this deserializer handles: sometimes
     * exact types, other time most specific supertype of
     * types deserializer handles (which may be as generic
     * as {@link Object} in some case)
     */
    final protected Class<?> _valueClass;

    protected StdDeserializer(Class<?> vc) {
        _valueClass = vc;
    }

    protected StdDeserializer(JavaType valueType) {
        _valueClass = (valueType == null) ? null : valueType.getRawClass();
    }

    /**
     * Copy-constructor for sub-classes to use, most often when creating
     * new instances for {@link com.fasterxml.jackson.databind.deser.ContextualDeserializer}.
     * 
     * @since 2.5
     */
    protected StdDeserializer(StdDeserializer<?> src) {
        _valueClass = src._valueClass;
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
     * Exact structured type deserializer handles, if known.
     *<p>
     * Default implementation just returns null.
     */
    public JavaType getValueType() { return null; }

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
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
    }
    
    /*
    /**********************************************************
    /* Helper methods for sub-classes, parsing: while mostly
    /* useful for numeric types, can be also useful for dealing
    /* with things serialized as numbers (such as Dates).
    /**********************************************************
     */

    protected final boolean _parseBooleanPrimitive(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_TRUE) return true;
        if (t == JsonToken.VALUE_FALSE) return false;
        if (t == JsonToken.VALUE_NULL) return false;

        // [JACKSON-78]: should accept ints too, (0 == false, otherwise true)
        if (t == JsonToken.VALUE_NUMBER_INT) {
            // 11-Jan-2012, tatus: May be outside of int...
            if (jp.getNumberType() == NumberType.INT) {
                return (jp.getIntValue() != 0);
            }
            return _parseBooleanFromNumber(jp, ctxt);
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            // [#422]: Allow aliases
            if ("true".equals(text) || "True".equals(text)) {
                return true;
            }
            if ("false".equals(text) || "False".equals(text) || text.length() == 0) {
                return false;
            }
            if (_hasTextualNull(text)) {
                return false;
            }
            throw ctxt.weirdStringException(text, _valueClass, "only \"true\" or \"false\" recognized");
        }
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final boolean parsed = _parseBooleanPrimitive(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'boolean' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final Boolean _parseBoolean(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (t == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        // [JACKSON-78]: should accept ints too, (0 == false, otherwise true)
        if (t == JsonToken.VALUE_NUMBER_INT) {
            // 11-Jan-2012, tatus: May be outside of int...
            if (p.getNumberType() == NumberType.INT) {
                return (p.getIntValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
            }
            return Boolean.valueOf(_parseBooleanFromNumber(p, ctxt));
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Boolean) getNullValue(ctxt);
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            // [#422]: Allow aliases
            if ("true".equals(text) || "True".equals(text)) {
                return Boolean.TRUE;
            }
            if ("false".equals(text) || "False".equals(text)) {
                return Boolean.FALSE;
            }
            if (text.length() == 0) {
                return (Boolean) getEmptyValue(ctxt);
            }
            if (_hasTextualNull(text)) {
                return (Boolean) getNullValue(ctxt);
            }
            throw ctxt.weirdStringException(text, _valueClass, "only \"true\" or \"false\" recognized");
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Boolean parsed = _parseBoolean(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Boolean' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final boolean _parseBooleanFromNumber(JsonParser jp, DeserializationContext ctxt)
            throws IOException
    {
        if (jp.getNumberType() == NumberType.LONG) {
            return (jp.getLongValue() == 0L) ? Boolean.FALSE : Boolean.TRUE;
        }
        // no really good logic; let's actually resort to textual comparison
        String str = jp.getText();
        if ("0.0".equals(str) || "0".equals(str)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    protected Byte _parseByte(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return p.getByteValue();
        }
        if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
            String text = p.getText().trim();
            if (_hasTextualNull(text)) {
                return (Byte) getNullValue(ctxt);
            }
            int value;
            try {
                int len = text.length();
                if (len == 0) {
                    return (Byte) getEmptyValue(ctxt);
                }
                value = NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(text, _valueClass, "not a valid Byte value");
            }
            // So far so good: but does it fit?
            // as per [JACKSON-804], allow range up to 255, inclusive
            if (value < Byte.MIN_VALUE || value > 255) {
                throw ctxt.weirdStringException(text, _valueClass, "overflow, value can not be represented as 8-bit value");
            }
            return Byte.valueOf((byte) value);
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "Byte");
            }
            return p.getByteValue();
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Byte) getNullValue(ctxt);
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Byte parsed = _parseByte(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Byte' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        throw ctxt.mappingException(_valueClass, t);
    }
    
    protected Short _parseShort(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return p.getShortValue();
        }
        if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
            String text = p.getText().trim();
            int value;
            try {
                int len = text.length();
                if (len == 0) {
                    return (Short) getEmptyValue(ctxt);
                }
                if (_hasTextualNull(text)) {
                    return (Short) getNullValue(ctxt);
                }
                value = NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(text, _valueClass, "not a valid Short value");
            }
            // So far so good: but does it fit?
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw ctxt.weirdStringException(text, _valueClass, "overflow, value can not be represented as 16-bit value");
            }
            return Short.valueOf((short) value);
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "Short");
            }
            return p.getShortValue();
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Short) getNullValue(ctxt);
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Short parsed = _parseShort(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Short' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final short _parseShortPrimitive(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        int value = _parseIntPrimitive(jp, ctxt);
        // So far so good: but does it fit?
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw ctxt.weirdStringException(String.valueOf(value),
                    _valueClass, "overflow, value can not be represented as 16-bit value");
        }
        return (short) value;
    }
    
    protected final int _parseIntPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return p.getIntValue();
        }
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) { // let's do implicit re-parse
            String text = p.getText().trim();
            if (_hasTextualNull(text)) {
                return 0;
            }
            try {
                int len = text.length();
                if (len > 9) {
                    long l = Long.parseLong(text);
                    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                        throw ctxt.weirdStringException(text, _valueClass,
                            "Overflow: numeric value ("+text+") out of range of int ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
                    }
                    return (int) l;
                }
                if (len == 0) {
                    return 0;
                }
                return NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(text, _valueClass, "not a valid int value");
            }
        }
        if (t == JsonToken.VALUE_NUMBER_FLOAT) {
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "int");
            }
            return p.getValueAsInt();
        }
        if (t == JsonToken.VALUE_NULL) {
            return 0;
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final int parsed = _parseIntPrimitive(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'int' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final Integer _parseInteger(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        switch (p.getCurrentTokenId()) {
        // NOTE: caller assumed to usually check VALUE_NUMBER_INT in fast path
        case JsonTokenId.ID_NUMBER_INT:
            return Integer.valueOf(p.getIntValue());
        case JsonTokenId.ID_NUMBER_FLOAT: // coercing may work too
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "Integer");
            }
            return Integer.valueOf(p.getValueAsInt());
        case JsonTokenId.ID_STRING: // let's do implicit re-parse
            String text = p.getText().trim();
            try {
                int len = text.length();
                if (_hasTextualNull(text)) {
                    return (Integer) getNullValue(ctxt);
                }
                if (len > 9) {
                    long l = Long.parseLong(text);
                    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
                        throw ctxt.weirdStringException(text, _valueClass,
                            "Overflow: numeric value ("+text+") out of range of Integer ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
                    }
                    return Integer.valueOf((int) l);
                }
                if (len == 0) {
                    return (Integer) getEmptyValue(ctxt);
                }
                return Integer.valueOf(NumberInput.parseInt(text));
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(text, _valueClass, "not a valid Integer value");
            }
        case JsonTokenId.ID_NULL:
            return (Integer) getNullValue(ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final Integer parsed = _parseInteger(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                            "Attempted to unwrap single value array for single 'Integer' value but there was more than a single value in the array");
                }            
                return parsed;            
            }
            break;
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, p.getCurrentToken());
    }

    protected final Long _parseLong(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        switch (p.getCurrentTokenId()) {
        // NOTE: caller assumed to usually check VALUE_NUMBER_INT in fast path
        case JsonTokenId.ID_NUMBER_INT:
            return p.getLongValue();
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "Long");
            }
            return p.getValueAsLong();
        case JsonTokenId.ID_STRING:
            // let's allow Strings to be converted too
            // !!! 05-Jan-2009, tatu: Should we try to limit value space, JDK is too lenient?
            String text = p.getText().trim();
            if (text.length() == 0) {
                return (Long) getEmptyValue(ctxt);
            }
            if (_hasTextualNull(text)) {
                return (Long) getNullValue(ctxt);
            }
            try {
                return Long.valueOf(NumberInput.parseLong(text));
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid Long value");
        case JsonTokenId.ID_NULL:
            return (Long) getNullValue(ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final Long parsed = _parseLong(p, ctxt);
                JsonToken t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                            "Attempted to unwrap single value array for single 'Long' value but there was more than a single value in the array");
                }            
                return parsed;            
            }
            break;
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, p.getCurrentToken());
    }

    protected final long _parseLongPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        switch (p.getCurrentTokenId()) {
        case JsonTokenId.ID_NUMBER_INT:
            return p.getLongValue();
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "long");
            }
            return p.getValueAsLong();
        case JsonTokenId.ID_STRING:
            String text = p.getText().trim();
            if (text.length() == 0 || _hasTextualNull(text)) {
                return 0L;
            }
            try {
                return NumberInput.parseLong(text);
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid long value");
        case JsonTokenId.ID_NULL:
            return 0L;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final long parsed = _parseLongPrimitive(p, ctxt);
            JsonToken t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'long' value but there was more than a single value in the array");
            }            
            return parsed;
            }
            break;
        }
        throw ctxt.mappingException(_valueClass, p.getCurrentToken());
    }
    
    protected final Float _parseFloat(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        // We accept couple of different types; obvious ones first:
        JsonToken t = jp.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return jp.getFloatValue();
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if (text.length() == 0) {
                return (Float) getEmptyValue(ctxt);
            }
            if (_hasTextualNull(text)) {
                return (Float) getNullValue(ctxt);
            }
            switch (text.charAt(0)) {
            case 'I':
                if (_isPosInf(text)) {
                    return Float.POSITIVE_INFINITY;
                }
                break;
            case 'N':
                if (_isNaN(text)) {
                    return Float.NaN;
                }
                break;
            case '-':
                if (_isNegInf(text)) {
                    return Float.NEGATIVE_INFINITY;
                }
                break;
            }
            try {
                return Float.parseFloat(text);
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid Float value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Float) getNullValue(ctxt);
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final Float parsed = _parseFloat(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Byte' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }
    
    protected final float _parseFloatPrimitive(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = jp.getCurrentToken();

        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return jp.getFloatValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if (text.length() == 0 || _hasTextualNull(text)) {
                return 0.0f;
            }
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
            }
            try {
                return Float.parseFloat(text);
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid float value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return 0.0f;
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final float parsed = _parseFloatPrimitive(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'float' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final Double _parseDouble(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return jp.getDoubleValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if (text.length() == 0) {
                return (Double) getEmptyValue(ctxt);
            }
            if (_hasTextualNull(text)) {
                return (Double) getNullValue(ctxt);
            }
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
            }
            try {
                return parseDouble(text);
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid Double value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Double) getNullValue(ctxt);
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final Double parsed = _parseDouble(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Double' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
            // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected final double _parseDoublePrimitive(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        // We accept couple of different types; obvious ones first:
        JsonToken t = jp.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return jp.getDoubleValue();
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if (text.length() == 0 || _hasTextualNull(text)) {
                return 0.0;
            }
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
            }
            try {
                return parseDouble(text);
            } catch (IllegalArgumentException iae) { }
            throw ctxt.weirdStringException(text, _valueClass, "not a valid double value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return 0.0;
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final double parsed = _parseDoublePrimitive(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'Byte' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
            // Otherwise, no can do:
        throw ctxt.mappingException(_valueClass, t);
    }

    protected java.util.Date _parseDate(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return new java.util.Date(jp.getLongValue());
        }
        if (t == JsonToken.VALUE_NULL) {
            return (java.util.Date) getNullValue(ctxt);
        }
        if (t == JsonToken.VALUE_STRING) {
            String value = null;
            try {
                // As per [JACKSON-203], take empty Strings to mean
                value = jp.getText().trim();
                if (value.length() == 0) {
                    return (Date) getEmptyValue(ctxt);
                }
                if (_hasTextualNull(value)) {
                    return (java.util.Date) getNullValue(ctxt);
                }
                return ctxt.parseDate(value);
            } catch (IllegalArgumentException iae) {
                throw ctxt.weirdStringException(value, _valueClass,
                        "not a valid representation (error: "+iae.getMessage()+")");
            }
        }
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final Date parsed = _parseDate(jp, ctxt);
            t = jp.nextToken();
            if (t != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'java.util.Date' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        throw ctxt.mappingException(_valueClass, t);
    }

    /**
     * Helper method for encapsulating calls to low-level double value parsing; single place
     * just because we need a work-around that must be applied to all calls.
     */
    protected final static double parseDouble(String numStr) throws NumberFormatException
    {
        // [JACKSON-486]: avoid some nasty float representations... but should it be MIN_NORMAL or MIN_VALUE?
        // for now, MIN_VALUE, since MIN_NORMAL is JDK 1.6
        if (NumberInput.NASTY_SMALL_DOUBLE.equals(numStr)) {
            return Double.MIN_VALUE;
        }
        return Double.parseDouble(numStr);
    }
    
    /**
     * Helper method used for accessing String value, if possible, doing
     * necessary conversion or throwing exception as necessary.
     * 
     * @since 2.1
     */
    protected final String _parseString(JsonParser jp, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            return jp.getText();
        }
        
        // Issue#381
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            jp.nextToken();
            final String parsed = _parseString(jp, ctxt);
            if (jp.nextToken() != JsonToken.END_ARRAY) {
                throw ctxt.wrongTokenException(jp, JsonToken.END_ARRAY, 
                        "Attempted to unwrap single value array for single 'String' value but there was more than a single value in the array");
            }            
            return parsed;            
        }
        String value = jp.getValueAsString();
        if (value != null) {
            return value;
        }
        throw ctxt.mappingException(String.class, jp.getCurrentToken());
    }

    /**
     * Helper method that may be used to support fallback for Empty String / Empty Array
     * non-standard representations; usually for things serialized as JSON Objects.
     * 
     * @since 2.5
     */
    protected T _deserializeFromEmpty(JsonParser jp, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_ARRAY) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
                t = jp.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                throw ctxt.mappingException(handledType(), JsonToken.START_ARRAY);
            }
        } else if (t == JsonToken.VALUE_STRING) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                String str = jp.getText().trim();
                if (str.isEmpty()) {
                    return null;
                }
            }
        }
        throw ctxt.mappingException(handledType());
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

    /*
    /****************************************************
    /* Helper methods for sub-classes, coercions
    /****************************************************
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
        int feats = ctxt.getDeserializationFeatures();
        if (DeserializationFeature.USE_BIG_INTEGER_FOR_INTS.enabledIn(feats)) {
            return p.getBigIntegerValue();
        }
        if (DeserializationFeature.USE_LONG_FOR_INTS.enabledIn(feats)) {
            return p.getLongValue();
        }
        return p.getBigIntegerValue(); // should be optimal, whatever it is
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
     *     for passing deserialized values; provided so deserializer can be contextualized if necessary (since 1.7)
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
     */
    protected final boolean _isIntNumber(String text)
    {
        final int len = text.length();
        if (len > 0) {
            char c = text.charAt(0);
            // skip leading sign (plus not allowed for strict JSON numbers but...)
            int i = (c == '-' || c == '+') ? 1 : 0;
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
    /* Helper methods for sub-classes, deserializer construction
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
        if (intr != null && prop != null) {
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
    /* Helper methods for sub-classes, problem reporting
    /**********************************************************
     */

    /**
     * Method called to deal with a property that did not map to a known
     * Bean property. Method can deal with the problem as it sees fit (ignore,
     * throw exception); but if it does return, it has to skip the matching
     * Json content parser has.
     *<p>
     * NOTE: method signature was changed in version 1.5; explicit JsonParser
     * <b>must</b> be passed since it may be something other than what
     * context has. Prior versions did not include the first parameter.
     *
     * @param jp Parser that points to value of the unknown property
     * @param ctxt Context for deserialization; allows access to the parser,
     *    error reporting functionality
     * @param instanceOrClass Instance that is being populated by this
     *   deserializer, or if not known, Class that would be instantiated.
     *   If null, will assume type is what {@link #getValueClass} returns.
     * @param propName Name of the property that can not be mapped
     */
    protected void handleUnknownProperty(JsonParser jp, DeserializationContext ctxt, Object instanceOrClass, String propName)
        throws IOException
    {
        if (instanceOrClass == null) {
            instanceOrClass = handledType();
        }
        // Maybe we have configured handler(s) to take care of it?
        if (ctxt.handleUnknownProperty(jp, this, instanceOrClass, propName)) {
            return;
        }
        // Nope, not handled. Potentially that's a problem...
        ctxt.reportUnknownProperty(instanceOrClass, propName, this);

        /* But if we do get this far, need to skip whatever value we
         * are pointing to now.
         */
        jp.skipChildren();
    }

    protected void _failDoubleToIntCoercion(JsonParser jp, DeserializationContext ctxt,
            String type) throws IOException
    {
        throw ctxt.mappingException("Can not coerce a floating-point value ('%s') into %s; enable `DeserializationFeature.ACCEPT_FLOAT_AS_INT` to allow",
                        jp.getValueAsString(), type);
    }
}

package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;
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
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        return typeDeserializer.deserializeTypedFromAny(p, ctxt);
    }
    
    /*
    /**********************************************************
    /* Helper methods for sub-classes, parsing: while mostly
    /* useful for numeric types, can be also useful for dealing
    /* with things serialized as numbers (such as Dates).
    /**********************************************************
     */

    protected final boolean _parseBooleanPrimitive(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_TRUE) return true;
        if (t == JsonToken.VALUE_FALSE) return false;
        if (t == JsonToken.VALUE_NULL) return false;

        // should accept ints too, (0 == false, otherwise true)
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return _parseBooleanFromInt(p, ctxt);
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            // [databind#422]: Allow aliases
            if ("true".equals(text) || "True".equals(text)) {
                return true;
            }
            if ("false".equals(text) || "False".equals(text) || text.length() == 0) {
                return false;
            }
            if (_hasTextualNull(text)) {
                return false;
            }
            Boolean b = (Boolean) ctxt.handleWeirdStringValue(_valueClass, text,
                    "only \"true\" or \"false\" recognized");
            return (b == null) ? false : b.booleanValue();
        }
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final boolean parsed = _parseBooleanPrimitive(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return ((Boolean) ctxt.handleUnexpectedToken(_valueClass, p)).booleanValue();
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
        // should accept ints too, (0 == false, otherwise true)
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return Boolean.valueOf(_parseBooleanFromInt(p, ctxt));
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Boolean) getNullValue(ctxt);
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
            // [databind#422]: Allow aliases
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
            return (Boolean) ctxt.handleWeirdStringValue(_valueClass, text,
                    "only \"true\" or \"false\" recognized");
        }
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Boolean parsed = _parseBoolean(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return (Boolean) ctxt.handleUnexpectedToken(_valueClass, p);
    }

    protected boolean _parseBooleanFromInt(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // 13-Oct-2016, tatu: As per [databind#1324], need to be careful wrt
        //    degenerate case of huge integers, legal in JSON.
        //  ... this is, on the other hand, probably wrong/sub-optimal for non-JSON
        //  input. For now, no rea

        // Anyway, note that since we know it's valid (JSON) integer, it can't have
        // extra whitespace to trim.
        return !"0".equals(p.getText());
    }

    @Deprecated // since 2.8.4
    protected boolean _parseBooleanFromOther(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        return _parseBooleanFromInt(p, ctxt);
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
                return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Byte value");
            }
            // So far so good: but does it fit?
            // as per [JACKSON-804], allow range up to 255, inclusive
            if (value < Byte.MIN_VALUE || value > 255) {
                return (Byte) ctxt.handleWeirdStringValue(_valueClass, text,
                        "overflow, value can not be represented as 8-bit value");
                // fall-through for deferred fails
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
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Byte parsed = _parseByte(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        return (Byte) ctxt.handleUnexpectedToken(_valueClass, p);
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
                return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Short value");
            }
            // So far so good: but does it fit?
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                return (Short) ctxt.handleWeirdStringValue(_valueClass, text,
                        "overflow, value can not be represented as 16-bit value");
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
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Short parsed = _parseShort(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        return (Short) ctxt.handleUnexpectedToken(_valueClass, p);
    }

    protected final short _parseShortPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        int value = _parseIntPrimitive(p, ctxt);
        // So far so good: but does it fit?
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, String.valueOf(value),
                    "overflow, value can not be represented as 16-bit value");
            return (v == null) ? (short) 0 : v.shortValue();
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
                        Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, text,
                            "Overflow: numeric value (%s) out of range of int (%d -%d)",
                            text, Integer.MIN_VALUE, Integer.MAX_VALUE);
                        return (v == null) ? 0 : v.intValue();
                    }
                    return (int) l;
                }
                if (len == 0) {
                    return 0;
                }
                return NumberInput.parseInt(text);
            } catch (IllegalArgumentException iae) {
                Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid int value");
                return (v == null) ? 0 : v.intValue();
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
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return ((Number) ctxt.handleUnexpectedToken(_valueClass, p)).intValue();
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
                        return (Integer) ctxt.handleWeirdStringValue(_valueClass, text,
                            "Overflow: numeric value ("+text+") out of range of Integer ("+Integer.MIN_VALUE+" - "+Integer.MAX_VALUE+")");
                        // fall-through
                    }
                    return Integer.valueOf((int) l);
                }
                if (len == 0) {
                    return (Integer) getEmptyValue(ctxt);
                }
                return Integer.valueOf(NumberInput.parseInt(text));
            } catch (IllegalArgumentException iae) {
                return (Integer) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid Integer value");
            }
            // fall-through
        case JsonTokenId.ID_NULL:
            return (Integer) getNullValue(ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final Integer parsed = _parseInteger(p, ctxt);
                if (p.nextToken() != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }            
                return parsed;            
            }
            break;
        }
        // Otherwise, no can do:
        return (Integer) ctxt.handleUnexpectedToken(_valueClass, p);
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
            return (Long) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid Long value");
            // fall-through
        case JsonTokenId.ID_NULL:
            return (Long) getNullValue(ctxt);
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final Long parsed = _parseLong(p, ctxt);
                JsonToken t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }            
                return parsed;            
            }
            break;
        }
        // Otherwise, no can do:
        return (Long) ctxt.handleUnexpectedToken(_valueClass, p);
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
            {
                Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, text,
                        "not a valid long value");
                return (v == null) ? 0 : v.longValue();
            }
        case JsonTokenId.ID_NULL:
            return 0L;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final long parsed = _parseLongPrimitive(p, ctxt);
                JsonToken t = p.nextToken();
                if (t != JsonToken.END_ARRAY) {
                    handleMissingEndArrayForSingle(p, ctxt);
                }
                return parsed;
            }
            break;
        }
        return ((Number) ctxt.handleUnexpectedToken(_valueClass, p)).longValue();
    }

    protected final Float _parseFloat(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // We accept couple of different types; obvious ones first:
        JsonToken t = p.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return p.getFloatValue();
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
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
            return (Float) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid Float value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Float) getNullValue(ctxt);
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Float parsed = _parseFloat(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return (Float) ctxt.handleUnexpectedToken(_valueClass, p);
    }

    protected final float _parseFloatPrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();

        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return p.getFloatValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
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
            Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid float value");
            return (v == null) ? 0 : v.floatValue();
        }
        if (t == JsonToken.VALUE_NULL) {
            return 0.0f;
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final float parsed = _parseFloatPrimitive(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return ((Number) ctxt.handleUnexpectedToken(_valueClass, p)).floatValue();
    }

    protected final Double _parseDouble(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return p.getDoubleValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
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
            return (Double) ctxt.handleWeirdStringValue(_valueClass, text,
                    "not a valid Double value");
        }
        if (t == JsonToken.VALUE_NULL) {
            return (Double) getNullValue(ctxt);
        }
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Double parsed = _parseDouble(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return (Double) ctxt.handleUnexpectedToken(_valueClass, p);
    }

    protected final double _parseDoublePrimitive(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        // We accept couple of different types; obvious ones first:
        JsonToken t = p.getCurrentToken();
        
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) { // coercing should work too
            return p.getDoubleValue();
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getText().trim();
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
            Number v = (Number) ctxt.handleWeirdStringValue(_valueClass, text, 
                    "not a valid double value");
            return (v == null) ? 0 : v.doubleValue();
        }
        if (t == JsonToken.VALUE_NULL) {
            return 0.0;
        }
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final double parsed = _parseDoublePrimitive(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        // Otherwise, no can do:
        return ((Number) ctxt.handleUnexpectedToken(_valueClass, p)).doubleValue();
    }

    protected java.util.Date _parseDate(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return new java.util.Date(p.getLongValue());
        }
        if (t == JsonToken.VALUE_NULL) {
            return (java.util.Date) getNullValue(ctxt);
        }
        if (t == JsonToken.VALUE_STRING) {
            return _parseDate(p.getText().trim(), ctxt);
        }
        // [databind#381]
        if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final Date parsed = _parseDate(p, ctxt);
            t = p.nextToken();
            if (t != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        return (java.util.Date)  ctxt.handleUnexpectedToken(_valueClass, p);
    }

    /**
     * @since 2.8
     */
    protected java.util.Date _parseDate(String value, DeserializationContext ctxt)
        throws IOException
    {
        try {
            // Take empty Strings to mean 'empty' Value, usually 'null':
            if (value.length() == 0) {
                return (Date) getEmptyValue(ctxt);
            }
            if (_hasTextualNull(value)) {
                return (java.util.Date) getNullValue(ctxt);
            }
            return ctxt.parseDate(value);
        } catch (IllegalArgumentException iae) {
            return (java.util.Date) ctxt.handleWeirdStringValue(_valueClass, value,
                    "not a valid representation (error: %s)", iae.getMessage());
        }
    }

    /**
     * Helper method for encapsulating calls to low-level double value parsing; single place
     * just because we need a work-around that must be applied to all calls.
     */
    protected final static double parseDouble(String numStr) throws NumberFormatException
    {
        // avoid some nasty float representations... but should it be MIN_NORMAL or MIN_VALUE?
        if (NumberInput.NASTY_SMALL_DOUBLE.equals(numStr)) {
            return Double.MIN_NORMAL; // since 2.7; was MIN_VALUE prior
        }
        return Double.parseDouble(numStr);
    }
    
    /**
     * Helper method used for accessing String value, if possible, doing
     * necessary conversion or throwing exception as necessary.
     * 
     * @since 2.1
     */
    protected final String _parseString(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            return p.getText();
        }
        // [databind#381]
        if ((t == JsonToken.START_ARRAY) && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
            p.nextToken();
            final String parsed = _parseString(p, ctxt);
            if (p.nextToken() != JsonToken.END_ARRAY) {
                handleMissingEndArrayForSingle(p, ctxt);
            }            
            return parsed;            
        }
        String value = p.getValueAsString();
        if (value != null) {
            return value;
        }
        return (String) ctxt.handleUnexpectedToken(String.class, p);
    }

    /**
     * Helper method that may be used to support fallback for Empty String / Empty Array
     * non-standard representations; usually for things serialized as JSON Objects.
     * 
     * @since 2.5
     */
    @SuppressWarnings("unchecked")
    protected T _deserializeFromEmpty(JsonParser p, DeserializationContext ctxt)
        throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.START_ARRAY) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)) {
                t = p.nextToken();
                if (t == JsonToken.END_ARRAY) {
                    return null;
                }
                return (T) ctxt.handleUnexpectedToken(handledType(), p);
            }
        } else if (t == JsonToken.VALUE_STRING) {
            if (ctxt.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)) {
                String str = p.getText().trim();
                if (str.isEmpty()) {
                    return null;
                }
            }
        }
        return (T) ctxt.handleUnexpectedToken(handledType(), p);
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
     * @param propName Name of the property that can not be mapped
     */
    protected void handleUnknownProperty(JsonParser p, DeserializationContext ctxt, Object instanceOrClass, String propName)
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
        ctxt.reportWrongTokenException(p, JsonToken.END_ARRAY, 
"Attempted to unwrap single value array for single '%s' value but there was more than a single value in the array",
handledType().getName());
        // 05-May-2016, tatu: Should recover somehow (maybe skip until END_ARRAY);
        //     but for now just fall through
    }

    protected void _failDoubleToIntCoercion(JsonParser p, DeserializationContext ctxt,
            String type) throws IOException
    {
        ctxt.reportMappingException("Can not coerce a floating-point value ('%s') into %s; enable `DeserializationFeature.ACCEPT_FLOAT_AS_INT` to allow",
                        p.getValueAsString(), type);
    }
}

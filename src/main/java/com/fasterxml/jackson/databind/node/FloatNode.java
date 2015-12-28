package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * <code>JsonNode</code> implementation for efficiently containing 32-bit
 * `float` values.
 * 
 * @since 2.2
 */
public class FloatNode extends NumericNode
{
    protected final float _value;

    /* 
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public FloatNode(float v) { _value = v; }

    public static FloatNode valueOf(float v) { return new FloatNode(v); }

    /* 
    /**********************************************************
    /* BaseJsonNode extended API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.FLOAT; }

    /* 
    /**********************************************************
    /* Overrridden JsonNode methods
    /**********************************************************
     */

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isFloat() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value >= Integer.MIN_VALUE && _value <= Integer.MAX_VALUE);
    }

    @Override public boolean canConvertToLong() {
        return (_value >= Long.MIN_VALUE && _value <= Long.MAX_VALUE);
    }
    
    @Override
    public Number numberValue() {
        return Float.valueOf(_value);
    }

    @Override
    public short shortValue() { return (short) _value; }

    @Override
    public int intValue() { return (int) _value; }

    @Override
    public long longValue() { return (long) _value; }

    @Override
    public float floatValue() { return _value; }
    
    @Override
    public double doubleValue() { return _value; }

    @Override
    public BigDecimal decimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigInteger bigIntegerValue() {
        return decimalValue().toBigInteger();
    }

    @Override
    public String asText() {
        // As per [jackson-databind#707]
//        return NumberOutput.toString(_value);
        // TODO: in 2.7, call `NumberOutput.toString (added in 2.6); not yet for backwards compat
        return Float.toString(_value);
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider) throws IOException
    {
        jg.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof FloatNode) {
            // We must account for NaNs: NaN does not equal NaN, therefore we have
            // to use Double.compare().
            final float otherValue = ((FloatNode) o)._value;
            return Float.compare(_value, otherValue) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits(_value);
    }
}

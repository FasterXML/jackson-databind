package com.fasterxml.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;

import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * {@code JsonNode} implementation for efficiently containing 32-bit
 * `float` values.
 */
public class FloatNode extends NumericNode
{
    private static final long serialVersionUID = 3L;

    protected final float _value;

    /* 
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public FloatNode(float v) { _value = v; }

    public static FloatNode valueOf(float v) { return new FloatNode(v); }

    /* 
    /**********************************************************************
    /* BaseJsonNode extended API
    /**********************************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.FLOAT; }

    /* 
    /**********************************************************************
    /* Overrridden JsonNode methods
    /**********************************************************************
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

    @Override // since 2.12
    public boolean canConvertToExactIntegral() {
        return !Float.isNaN(_value) && !Float.isInfinite(_value)
                && (_value == Math.round(_value));
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
        return String.valueOf(_value);
    }

    @Override
    public boolean isNaN() {
        return NumberOutput.notFinite(_value);
    }

    @Override
    public final void serialize(JsonGenerator g, SerializerProvider provider)
            throws JacksonException {
        g.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof FloatNode) {
            // We must account for NaNs: NaN does not equal NaN, therefore we have
            // to use Float.compare().
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

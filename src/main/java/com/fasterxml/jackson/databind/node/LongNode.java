package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.NumberOutput;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Numeric node that contains simple 64-bit integer values.
 */
@SuppressWarnings("serial")
public class LongNode
    extends NumericNode
{
    protected final long _value;

    /*
    ************************************************
    * Construction
    ************************************************
    */

    public LongNode(long v) { _value = v; }

    public static LongNode valueOf(long l) { return new LongNode(l); }

    /*
    ************************************************
    * Overrridden JsonNode methods
    ************************************************
    */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.LONG; }


    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isLong() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value >= Integer.MIN_VALUE && _value <= Integer.MAX_VALUE);
    }
    @Override public boolean canConvertToLong() { return true; }

    @Override
    public Number numberValue() {
        return Long.valueOf(_value);
    }

    @Override
    public short shortValue() { return (short) _value; }

    @Override
    public int intValue() { return (int) _value; }

    @Override
    public long longValue() { return _value; }

    @Override
    public float floatValue() { return _value; }

    @Override
    public double doubleValue() { return _value; }

    @Override
    public BigDecimal decimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigInteger bigIntegerValue() { return BigInteger.valueOf(_value); }

    @Override
    public String asText() {
        return NumberOutput.toString(_value);
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return _value != 0;
    }

    @Override
    public final void serialize(JsonGenerator g, SerializerProvider provider)
        throws IOException
    {
        g.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof LongNode) {
            return ((LongNode) o)._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((int) _value) ^ (int) (_value >> 32);
    }
}

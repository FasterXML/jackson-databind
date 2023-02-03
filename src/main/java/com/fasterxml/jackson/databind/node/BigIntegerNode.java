package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
@SuppressWarnings("serial")
public class BigIntegerNode
    extends NumericNode
{
    private final static BigInteger MIN_INTEGER = BigInteger.valueOf(Integer.MIN_VALUE);
    private final static BigInteger MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private final static BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    private final static BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    final protected BigInteger _value;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public BigIntegerNode(BigInteger v) { _value = v; }

    public static BigIntegerNode valueOf(BigInteger v) { return new BigIntegerNode(v); }

    /*
    /**********************************************************
    /* Overridden JsonNode methods
    /**********************************************************
     */

    @Override
    public JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.BIG_INTEGER; }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isBigInteger() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value.compareTo(MIN_INTEGER) >= 0) && (_value.compareTo(MAX_INTEGER) <= 0);
    }
    @Override public boolean canConvertToLong() {
        return (_value.compareTo(MIN_LONG) >= 0) && (_value.compareTo(MAX_LONG) <= 0);
    }

    @Override
    public Number numberValue() {
        return _value;
    }

    @Override
    public short shortValue() { return _value.shortValue(); }

    @Override
    public int intValue() { return _value.intValue(); }

    @Override
    public long longValue() { return _value.longValue(); }

    @Override
    public BigInteger bigIntegerValue() { return _value; }

    @Override
    public float floatValue() { return _value.floatValue(); }

    @Override
    public double doubleValue() { return _value.doubleValue(); }

    @Override
    public BigDecimal decimalValue() { return new BigDecimal(_value); }

    /*
    /**********************************************************
    /* General type coercions
    /**********************************************************
     */

    @Override
    public String asText() {
        return _value.toString();
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return !BigInteger.ZERO.equals(_value);
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
        if (!(o instanceof BigIntegerNode)) {
            return false;
        }
        return ((BigIntegerNode) o)._value.equals(_value);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }
}

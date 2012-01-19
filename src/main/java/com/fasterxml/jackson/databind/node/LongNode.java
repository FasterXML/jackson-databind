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
public final class LongNode
    extends NumericNode
{
    final long _value;

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
    public JsonParser.NumberType getNumberType() { return JsonParser.NumberType.LONG; }


    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isLong() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value >= Integer.MIN_VALUE && _value <= Integer.MAX_VALUE);
    }
    @Override public boolean canConvertToLong() { return true; }
    
    @Override
    public Number getNumberValue() {
        return Long.valueOf(_value);
    }

    @Override
    public int getIntValue() { return (int) _value; }

    @Override
    public long getLongValue() { return _value; }

    @Override
    public double getDoubleValue() { return (double) _value; }

    @Override
    public BigDecimal getDecimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigInteger getBigIntegerValue() { return BigInteger.valueOf(_value); }

    @Override
    public String asText() {
        return NumberOutput.toString(_value);
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        return _value != 0;
    }
    
    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((LongNode) o)._value == _value;
    }

    @Override
    public int hashCode() {
        return ((int) _value) ^ (int) (_value >> 32);
    }
}

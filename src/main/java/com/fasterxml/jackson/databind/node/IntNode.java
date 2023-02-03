package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.NumberOutput;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Numeric node that contains simple 32-bit integer values.
 */
@SuppressWarnings("serial")
public class IntNode
    extends NumericNode
{
    // // // Let's cache small set of common value

    final static int MIN_CANONICAL = -1;
    final static int MAX_CANONICAL = 10;

    private final static IntNode[] CANONICALS;
    static {
        int count = MAX_CANONICAL - MIN_CANONICAL + 1;
        CANONICALS = new IntNode[count];
        for (int i = 0; i < count; ++i) {
            CANONICALS[i] = new IntNode(MIN_CANONICAL + i);
        }
    }

    /**
     * Integer value this node contains
     */
    protected final int _value;

    /*
    ************************************************
    * Construction
    ************************************************
    */

    public IntNode(int v) { _value = v; }

    public static IntNode valueOf(int i) {
        if (i > MAX_CANONICAL || i < MIN_CANONICAL) return new IntNode(i);
        return CANONICALS[i - MIN_CANONICAL];
    }

    /*
    /**********************************************************
    /* BaseJsonNode extended API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.INT; }

    /*
    /**********************************************************
    /* Overrridden JsonNode methods
    /**********************************************************
     */

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isInt() { return true; }

    @Override public boolean canConvertToInt() { return true; }
    @Override public boolean canConvertToLong() { return true; }

    @Override
    public Number numberValue() {
        return Integer.valueOf(_value);
    }

    @Override
    public short shortValue() { return (short) _value; }

    @Override
    public int intValue() { return _value; }

    @Override
    public long longValue() { return (long) _value; }

    @Override
    public float floatValue() { return (float) _value; }

    @Override
    public double doubleValue() { return (double) _value; }


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
        if (o instanceof IntNode) {
            return ((IntNode) o)._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() { return _value; }
}

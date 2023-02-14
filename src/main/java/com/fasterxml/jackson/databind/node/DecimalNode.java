package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

/**
 * Numeric node that contains values that do not fit in simple
 * integer (int, long) or floating point (double) values.
 */
@SuppressWarnings("serial")
public class DecimalNode
    extends NumericNode
{
    public static final DecimalNode ZERO = new DecimalNode(BigDecimal.ZERO);

    private final static BigDecimal MIN_INTEGER = BigDecimal.valueOf(Integer.MIN_VALUE);
    private final static BigDecimal MAX_INTEGER = BigDecimal.valueOf(Integer.MAX_VALUE);
    private final static BigDecimal MIN_LONG = BigDecimal.valueOf(Long.MIN_VALUE);
    private final static BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    final protected BigDecimal _value;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public DecimalNode(BigDecimal v) { _value = v; }

    public static DecimalNode valueOf(BigDecimal d) { return new DecimalNode(d); }

    /*
    /**********************************************************
    /* BaseJsonNode extended API
    /**********************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.BIG_DECIMAL; }

    /*
    /**********************************************************
    /* Overrridden JsonNode methods
    /**********************************************************
     */

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isBigDecimal() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value.compareTo(MIN_INTEGER) >= 0) && (_value.compareTo(MAX_INTEGER) <= 0);
    }
    @Override public boolean canConvertToLong() {
        return (_value.compareTo(MIN_LONG) >= 0) && (_value.compareTo(MAX_LONG) <= 0);
    }

    @Override // since 2.12
    public boolean canConvertToExactIntegral() {
        return (_value.signum() == 0)
                || (_value.scale() <= 0)
                || (_value.stripTrailingZeros().scale() <= 0);
    }

    @Override
    public Number numberValue() { return _value; }

    @Override
    public short shortValue() { return _value.shortValue(); }

    @Override
    public int intValue() { return _value.intValue(); }

    @Override
    public long longValue() { return _value.longValue(); }


    @Override
    public BigInteger bigIntegerValue() { return _value.toBigInteger(); }

    @Override
    public float floatValue() { return _value.floatValue(); }

    @Override
    public double doubleValue() { return _value.doubleValue(); }

    @Override
    public BigDecimal decimalValue() { return _value; }

    @Override
    public String asText() {
        return _value.toString();
    }

    @Override
    public final void serialize(JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        // 07-Jul-2013, tatu: Should be handled by propagating setting to JsonGenerator
        //    so this should not be needed:
        /*
        if (provider.isEnabled(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)) {
            if (!(jgen instanceof TokenBuffer)) { // [Issue#232]
                jgen.writeNumber(((BigDecimal) _value).toPlainString());
                return;
            }
        }
        */
        jgen.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof DecimalNode) {
            return ((DecimalNode) o)._value.compareTo(_value) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() { return Double.valueOf(doubleValue()).hashCode(); }
}

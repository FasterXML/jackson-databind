package com.fasterxml.jackson.databind.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Numeric node for large decimal numeric values
 *
 * <p>The numeric value is stored as a {@link BigDecimal}, with all its
 * pecularities; the first of which is that two instances of BigDecimal with the
 * same <i>numeric</i> value may not be {@link Object#equals(Object) equal}. For
 * instance:</p>
 *
 * <pre>
 *     new BigDecimal("1.0").equals(new BigDecimal("1")); // returns false
 * </pre>
 *
 * <p>However:</p>
 *
 * <pre>
 *     new BigDecimal("1.0").compareTo(new BigDecimal("1")); // returns 0
 * </pre>
 *
 * <p>In order to respect the {@code .equals()/.hashCode()} contract that all
 * other {@link JsonNode} implementations do, {@link #hashCode()} is implemented
 * by returning the hashcode of this BigDecimal's {@link
 * BigDecimal#doubleValue() .doubleValue()}, which returns the same value for
 * two instances which are numerically equal, and {@link #equals(Object)} is
 * implemented by the means of {@link BigDecimal#compareTo(Object)}.</p>
 */
public final class DecimalNode
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
        return ((DecimalNode) o)._value.compareTo(_value) == 0;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(_value.doubleValue()).hashCode();
    }
}

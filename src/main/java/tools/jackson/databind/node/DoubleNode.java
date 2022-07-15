package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.databind.SerializerProvider;

/**
 * Numeric node that contains 64-bit ("double precision")
 * floating point values simple 32-bit integer values.
 */
public class DoubleNode
    extends NumericNode
{
    private static final long serialVersionUID = 3L;

    protected final double _value;

    /* 
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DoubleNode(double v) { _value = v; }

    public static DoubleNode valueOf(double v) { return new DoubleNode(v); }

    /* 
    /**********************************************************************
    /* BaseJsonNode extended API
    /**********************************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.DOUBLE; }

    /* 
    /**********************************************************************
    /* Overrridden JsonNode methods
    /**********************************************************************
     */

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isDouble() { return true; }

    @Override public boolean canConvertToInt() {
        return (_value >= Integer.MIN_VALUE && _value <= Integer.MAX_VALUE);
    }
    @Override public boolean canConvertToLong() {
        return (_value >= Long.MIN_VALUE && _value <= Long.MAX_VALUE);
    }

    @Override // since 2.12
    public boolean canConvertToExactIntegral() {
        return !Double.isNaN(_value) && !Double.isInfinite(_value)
                && (_value == Math.rint(_value));
    }

    @Override
    public Number numberValue() {
        return Double.valueOf(_value);
    }

    @Override
    public short shortValue() { return (short) _value; }

    @Override
    public int intValue() { return (int) _value; }

    @Override
    public long longValue() { return (long) _value; }

    @Override
    public float floatValue() { return (float) _value; }

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
        if (o instanceof DoubleNode) {
            // We must account for NaNs: NaN does not equal NaN, therefore we have
            // to use Double.compare().
            final double otherValue = ((DoubleNode) o)._value;
            return Double.compare(_value, otherValue) == 0;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        // same as hashCode Double.class uses
        long l = Double.doubleToLongBits(_value);
        return ((int) l) ^ (int) (l >> 32);

    }
}

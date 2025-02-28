package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalInt;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.databind.SerializationContext;

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
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isDouble() { return true; }

    @Override public boolean canConvertToInt() {
        return canConvertToExactIntegral() && _inIntRange();
    }

    @Override public boolean canConvertToLong() {
        return canConvertToExactIntegral() && _inLongRange();
    }

    @Override
    public boolean canConvertToExactIntegral() {
        return !isNaN() && !_hasFractionalPart();
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    public Number numberValue() {
        return Double.valueOf(_value);
    }

    @Override
    public short shortValue() {
        if (!_inShortRange()) {
            return _reportShortCoercionRangeFail("shortValue()");
        }
        if (_hasFractionalPart()) {
            _reportCoercionFail("shortValue()", Short.TYPE,
                    "value has fractional part");
        }
        return (short) _value;
    }

    @Override
    public int intValue() {
        if (!_inIntRange()) {
            return _reportIntCoercionRangeFail("intValue()");
        }
        if (_hasFractionalPart()) {
            _reportCoercionFail("intValue()", Integer.TYPE,
                    "value has fractional part");
        }
        return (int) _value;
    }

    @Override
    public int intValue(int defaultValue) {
        if (!_inIntRange() || _hasFractionalPart()) {
             return defaultValue;
        }
        return (int) _value;
    }

    @Override
    public OptionalInt intValueOpt() {
        if (!_inIntRange() || _hasFractionalPart()) {
            return OptionalInt.empty();
       }
       return OptionalInt.of((int) _value);
    }
    
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
    public String asString() {
        return String.valueOf(_value);
    }

    @Override
    public boolean isNaN() {
        return NumberOutput.notFinite(_value);
    }

    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
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

    private boolean _hasFractionalPart() { return _value != Math.rint(_value); }

    private boolean _inShortRange() {
        return !isNaN() && (_value >= Short.MIN_VALUE) && (_value <= Short.MAX_VALUE);
    }

    private boolean _inIntRange() {
        return !isNaN() &&(_value >= Integer.MIN_VALUE) && (_value <= Integer.MAX_VALUE);
    }

    private boolean _inLongRange() {
        return !isNaN() && (_value >= Long.MIN_VALUE) && (_value <= Long.MAX_VALUE);
    }
}

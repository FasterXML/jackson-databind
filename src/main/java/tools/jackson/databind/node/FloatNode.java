package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalInt;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.databind.SerializationContext;

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
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.FLOAT; }

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isFloat() { return true; }

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
        return Float.valueOf(_value);
    }

    @Override
    public short shortValue() { return (short) _value; }

    @Override
    public int intValue() {
        if (isNaN() || !_inIntRange()) {
            _reportCoercionFail("intValue()", Integer.TYPE,
                    "value not in 32-bit `int` range");
        }
        if (_hasFractionalPart()) {
            _reportCoercionFail("intValue()", Integer.TYPE,
                    "value has fractional part");
        }
        return (int) _value;
    }

    @Override
    public int intValue(int defaultValue) {
        if (isNaN() || !_inIntRange() || _hasFractionalPart()) {
             return defaultValue;
        }
        return (int) _value;
    }

    @Override
    public OptionalInt intValueOpt() {
        if (isNaN() || !_inIntRange() || _hasFractionalPart()) {
            return OptionalInt.empty();
       }
       return OptionalInt.of((int) _value);
    }
    
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

    private boolean _hasFractionalPart() { return _value != Math.round(_value); }

    private boolean _inIntRange() {
        return (_value >= Integer.MIN_VALUE) && (_value <= Integer.MAX_VALUE);
    }

    private boolean _inLongRange() {
        return (_value >= Long.MIN_VALUE) && (_value <= Long.MAX_VALUE);
    }
}

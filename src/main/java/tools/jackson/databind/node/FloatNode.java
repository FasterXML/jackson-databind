package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

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
    protected String _asString() {
        return String.valueOf(_value);
    }

    @Override
    public Number numberValue() {
        return Float.valueOf(_value);
    }

    @Override
    public short shortValue() {
        if (!_inShortRange()) {
            _reportShortCoercionRangeFail("shortValue()");
        }
        if (_hasFractionalPart()) {
            _reportShortCoercionFractionFail("shortValue()");
        }
        return (short) _value;
    }

    @Override
    public int intValue() {
        if (!_inIntRange()) {
            _reportIntCoercionRangeFail("intValue()");
        }
        if (_hasFractionalPart()) {
            _reportIntCoercionFractionFail("intValue()");
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
    public long longValue() {
        if (!_inLongRange()) {
            _reportLongCoercionRangeFail("longValue()");
        }
        if (_hasFractionalPart()) {
            _reportLongCoercionFractionFail("longValue()");
        }
        return (long) _value;
    }

    @Override
    public long longValue(long defaultValue) {
        if (!_inLongRange() || _hasFractionalPart()) {
            return defaultValue;
       }
        return (long) _value;
    }

    @Override
    public OptionalLong longValueOpt() {
        if (!_inLongRange() || _hasFractionalPart()) {
            return OptionalLong.empty();
       }
       return OptionalLong.of((long) _value);
    }

    @Override
    public BigInteger bigIntegerValue() {
        if (_hasFractionalPart()) {
            _reportBigIntegerCoercionFractionFail("bigIntegerValue()");
        }
        return decimalValue().toBigInteger();
    }

    @Override
    public BigInteger bigIntegerValue(BigInteger defaultValue) {
        if (_hasFractionalPart()) {
            return defaultValue;
        }
        return decimalValue().toBigInteger();
    }

    @Override
    public Optional<BigInteger> bigIntegerValueOpt() {
        if (_hasFractionalPart()) {
            return Optional.empty();
        }
        return Optional.of(decimalValue().toBigInteger());
    }
    
    @Override
    public float floatValue() { return _value; }

    @Override
    public double doubleValue() { return _value; }

    @Override
    public double doubleValue(double defaultValue) { return _value; }

    @Override
    public OptionalDouble doubleValueOpt() {
        return OptionalDouble.of(_value);
    }

    @Override
    public BigDecimal decimalValue() {
        if (isNaN()) {
            _reportBigDecimalCoercionNaNFail("decimalValue()");
        }
        return BigDecimal.valueOf(_value);
    }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) {
        if (isNaN()) {
            return defaultValue;
        }
        return BigDecimal.valueOf(_value);
    }

    @Override
    public Optional<BigDecimal> decimalValueOpt() {
        if (isNaN()) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(_value));
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

    private boolean _inShortRange() {
        return !isNaN() && (_value >= Short.MIN_VALUE) && (_value <= Short.MAX_VALUE);
    }

    private boolean _inIntRange() {
        return !isNaN() && (_value >= Integer.MIN_VALUE) && (_value <= Integer.MAX_VALUE);
    }

    private boolean _inLongRange() {
        return !isNaN() && (_value >= Long.MIN_VALUE) && (_value <= Long.MAX_VALUE);
    }
}

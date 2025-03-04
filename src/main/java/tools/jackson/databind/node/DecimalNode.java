package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.*;

/**
 * Numeric node that contains values that do not fit in simple
 * integer (int, long) or floating point (double) values.
 */
public class DecimalNode
    extends NumericNode
{
    private static final long serialVersionUID = 3L;

    public static final DecimalNode ZERO = new DecimalNode(BigDecimal.ZERO);

    private final static BigDecimal MIN_SHORT = BigDecimal.valueOf(Short.MIN_VALUE);
    private final static BigDecimal MAX_SHORT = BigDecimal.valueOf(Short.MAX_VALUE);
    private final static BigDecimal MIN_INTEGER = BigDecimal.valueOf(Integer.MIN_VALUE);
    private final static BigDecimal MAX_INTEGER = BigDecimal.valueOf(Integer.MAX_VALUE);
    private final static BigDecimal MIN_LONG = BigDecimal.valueOf(Long.MIN_VALUE);
    private final static BigDecimal MAX_LONG = BigDecimal.valueOf(Long.MAX_VALUE);

    final protected BigDecimal _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DecimalNode(BigDecimal v) {
        // 01-Mar-2024, tatu: [databind#4381] No null-valued JsonNodes
        _value = Objects.requireNonNull(v);
    }

    public static DecimalNode valueOf(BigDecimal d) { return new DecimalNode(d); }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }
    
    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.BIG_DECIMAL; }

    @Override
    public boolean isBigDecimal() { return true; }

    @Override
    public boolean isFloatingPointNumber() { return true; }

    @Override
    public boolean isNaN() { return false; }
    
    @Override public boolean canConvertToInt() {
        return _inIntRange() && canConvertToExactIntegral();
    }

    @Override public boolean canConvertToLong() {
        return _inLongRange() && canConvertToExactIntegral();
    }

    @Override // since 2.12
    public boolean canConvertToExactIntegral() {
        return !_hasFractionalPart();
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    public Number numberValue() { return _value; }

    @Override
    public short shortValue() {
        if (!_inShortRange()) {
            return _reportShortCoercionRangeFail("shortValue()");
        }
        if (_hasFractionalPart()) {
            _reportShortCoercionFractionFail("shortValue()");
        }
        return _value.shortValue();
    }

    @Override
    public int intValue() {
        if (!_inIntRange()) {
            return _reportIntCoercionRangeFail("intValue()");
        }
        if (_hasFractionalPart()) {
            _reportIntCoercionFractionFail("intValue()");
        }
        return _value.intValue();
    }

    @Override
    public int intValue(int defaultValue) {
        if (!_inIntRange() || _hasFractionalPart()) {
             return defaultValue;
        }
        return _value.intValue();
    }

    @Override
    public OptionalInt intValueOpt() {
        if (!_inIntRange() || _hasFractionalPart()) {
            return OptionalInt.empty();
       }
       return OptionalInt.of(_value.intValue());
    }

    @Override
    public long longValue() {
        if (!_inLongRange()) {
            return _reportLongCoercionRangeFail("longValue()");
        }
        if (_hasFractionalPart()) {
            _reportLongCoercionFractionFail("longValue()");
        }
        return _value.longValue();
    }

    @Override
    public long longValue(long defaultValue) {
        if (!_inLongRange() || _hasFractionalPart()) {
            return defaultValue;
        }
        return _value.longValue();
    }

    @Override
    public OptionalLong longValueOpt() {
        if (!_inLongRange() || _hasFractionalPart()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(_value.longValue());
    }

    @Override
    public BigInteger bigIntegerValue() {
        if (_hasFractionalPart()) {
            _reportBigIntegerCoercionFractionFail("bigIntegerValue()");
        }
        return _value.toBigInteger();
    }

    @Override
    public float floatValue() {
        float f = _value.floatValue();
        if (Float.isFinite(f)) {
            return f;
        }
        return _reportFloatCoercionRangeFail("floatValue()");
    }

    @Override
    public double doubleValue() {
        double d = _value.doubleValue();
        if (Double.isFinite(d)) {
            return d;
        }
        return _reportDoubleCoercionRangeFail("doubleValue()");
    }

    @Override
    public double doubleValue(double defaultValue) {
        double d = _value.doubleValue();
        if (Double.isFinite(d)) {
            return d;
        }
        return defaultValue;
    }

    @Override
    public OptionalDouble doubleValueOpt() {
        double d = _value.doubleValue();
        if (Double.isFinite(d)) {
            return OptionalDouble.of(_value.doubleValue());
        }
        return OptionalDouble.empty();
    }

    @Override
    public BigDecimal decimalValue() { return _value; }

    @Override
    public String asString() {
        return _value.toString();
    }

    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        g.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof DecimalNode) {
            DecimalNode otherNode = (DecimalNode) o;
            return otherNode._value.equals(_value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }

    private boolean _hasFractionalPart() {
        return (_value.signum() != 0)
               && (_value.scale() > 0)
               && (_value.stripTrailingZeros().scale() > 0);
    }
    
    private boolean _inShortRange() {
        return (_value.compareTo(MIN_SHORT) >= 0) && (_value.compareTo(MAX_SHORT) <= 0);
    }

    private boolean _inIntRange() {
        return (_value.compareTo(MIN_INTEGER) >= 0) && (_value.compareTo(MAX_INTEGER) <= 0);
    }

    private boolean _inLongRange() {
        return (_value.compareTo(MIN_LONG) >= 0) && (_value.compareTo(MAX_LONG) <= 0);
    }
}

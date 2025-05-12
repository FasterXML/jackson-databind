package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.SerializationContext;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
public class BigIntegerNode
    extends NumericIntNode
{
    private static final long serialVersionUID = 3L;

    final protected BigInteger _value;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    public BigIntegerNode(BigInteger v) {
        // 01-Mar-2024, tatu: [databind#4381] No null-valued JsonNodes
        _value = Objects.requireNonNull(v);
    }

    public static BigIntegerNode valueOf(BigInteger v) { return new BigIntegerNode(v); }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.BIG_INTEGER; }

    @Override
    public boolean isBigInteger() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean() {
        return !BigInteger.ZERO.equals(_value);
    }

    @Override
    public String _asString() {
        return _value.toString();
    }

    @Override
    public Number numberValue() {
        return _value;
    }

    @Override
    public short shortValue() {
        if (_inShortRange()) {
            return _value.shortValue();
        }
        return _reportShortCoercionRangeFail("shortValue()");
    }

    @Override
    public short shortValue(short defaultValue) {
        return _inShortRange() ? _value.shortValue() : defaultValue;
    }

    @Override
    public Optional<Short> shortValueOpt() {
        return _inShortRange() ? Optional.of(_value.shortValue()) : Optional.empty();
    }

    @Override
    public short asShort() {
        if (_inShortRange()) {
            return _value.shortValue();
        }
        return _reportShortCoercionRangeFail("asShort()");
    }

    @Override
    public short asShort(short defaultValue) {
        return _inShortRange() ? _value.shortValue() : defaultValue;
    }

    @Override
    public Optional<Short> asShortOpt() {
        return _inShortRange() ? Optional.of(_value.shortValue()) : Optional.empty();
    }

    @Override
    public int intValue() {
        if (_inIntRange()) {
            return _value.intValue();
        }
        return _reportIntCoercionRangeFail("intValue()");
    }

    @Override
    public int intValue(int defaultValue) {
        return _inIntRange() ? _value.intValue() : defaultValue;
    }

    @Override
    public OptionalInt intValueOpt() {
        return _inIntRange() ? OptionalInt.of(_value.intValue()) : OptionalInt.empty();
    }
    
    @Override
    public int asInt() {
        if (_inIntRange()) {
            return _value.intValue();
        }
        return _reportIntCoercionRangeFail("asInt()");
    }

    @Override
    public int asInt(int defaultValue) {
        return _inIntRange() ? _value.intValue() : defaultValue;
    }

    @Override
    public OptionalInt asIntOpt() {
        return _inIntRange() ? OptionalInt.of(_value.intValue()) : OptionalInt.empty();
    }
    
    @Override
    public long longValue() {
        if (canConvertToLong()) {
            return _value.longValue();
        }
        return _reportLongCoercionRangeFail("longValue()");
    }

    @Override
    public long longValue(long defaultValue) {
        return (canConvertToLong()) ? _value.longValue() : defaultValue;
    }

    @Override
    public OptionalLong longValueOpt() {
        return canConvertToLong() ? OptionalLong.of(_value.longValue()) : OptionalLong.empty();
    }

    @Override
    public long asLong() {
        if (canConvertToLong()) {
            return _value.longValue();
        }
        return _reportLongCoercionRangeFail("asLong()");
    }

    @Override
    public long asLong(long defaultValue) {
        return (canConvertToLong()) ? _value.longValue() : defaultValue;
    }

    @Override
    public OptionalLong asLongOpt() {
        return canConvertToLong() ? OptionalLong.of(_value.longValue()) : OptionalLong.empty();
    }

    @Override
    public BigInteger bigIntegerValue() { return _value; }

    @Override
    public BigInteger bigIntegerValue(BigInteger defaultValue) { return _value; }

    @Override
    public Optional<BigInteger> bigIntegerValueOpt() { return Optional.of(_value); }

    
    // // // BigInteger differs a bit from other Integral types as there's
    // // // range overflow possibility

    @Override
    public float floatValue() {
        float f = _asFloatValueUnchecked();
        if (Float.isFinite(f)) {
            return f;
        }
        return _reportFloatCoercionRangeFail("floatValue()");
    }

    @Override
    public float floatValue(float defaultValue) {
        float f = _asFloatValueUnchecked();
        return (Float.isFinite(f)) ? f : defaultValue;
    }

    @Override
    public Optional<Float> floatValueOpt() {
        float f = _asFloatValueUnchecked();
        if (Float.isFinite(f)) {
            return Optional.of(f);
        }
        return Optional.empty();
    }

    @Override
    public float asFloat() {
        float f = _asFloatValueUnchecked();
        if (Float.isFinite(f)) {
            return f;
        }
        return _reportFloatCoercionRangeFail("asFloat()");
    }

    @Override
    public float asFloat(float defaultValue) {
        float f = _asFloatValueUnchecked();
        return (Float.isFinite(f)) ? f : defaultValue;
    }

    @Override
    public Optional<Float> asFloatOpt() {
        float f = _asFloatValueUnchecked();
        if (Float.isFinite(f)) {
            return Optional.of(f);
        }
        return Optional.empty();
    }

    @Override
    public double doubleValue() {
        double d = _asDoubleValueUnchecked();
        if (Double.isFinite(d)) {
            return d;
        }
        return _reportDoubleCoercionRangeFail("doubleValue()");
    }

    @Override
    public double doubleValue(double defaultValue) {
        double d = _asDoubleValueUnchecked();
        return (Double.isFinite(d)) ? d : defaultValue;
    }

    @Override
    public OptionalDouble doubleValueOpt() {
        double d = _asDoubleValueUnchecked();
        if (Double.isFinite(d)) {
            return OptionalDouble.of(d);
        }
        return OptionalDouble.empty();
    }

    @Override
    public double asDouble() {
        double d = _asDoubleValueUnchecked();
        if (Double.isFinite(d)) {
            return d;
        }
        return _reportDoubleCoercionRangeFail("asDouble()");
    }

    @Override
    public double asDouble(double defaultValue) {
        double d = _asDoubleValueUnchecked();
        return (Double.isFinite(d)) ? d : defaultValue;
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        double d = _asDoubleValueUnchecked();
        if (Double.isFinite(d)) {
            return OptionalDouble.of(d);
        }
        return OptionalDouble.empty();
    }

    @Override
    public BigDecimal decimalValue() {
        return new BigDecimal(_value);
    }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) {
        return new BigDecimal(_value);
    }

    @Override
    public Optional<BigDecimal> decimalValueOpt() {
        return Optional.of(new BigDecimal(_value));
    }

    /*
    /**********************************************************************
    /* Abstract methods impls for NumericIntNode
    /**********************************************************************
     */

    @Override
    protected int _asIntValueUnchecked() {
        return _value.intValue();
    }

    @Override
    protected float _asFloatValueUnchecked() {
        return _value.floatValue();
    }

    @Override
    protected double _asDoubleValueUnchecked() {
        return _value.doubleValue();
    }

    @Override
    protected boolean _inShortRange() {
        return (_value.compareTo(BI_MIN_SHORT) >= 0)
                && (_value.compareTo(BI_MAX_SHORT) <= 0);
    }

    @Override
    public boolean _inIntRange() {
        return (_value.compareTo(BI_MIN_INTEGER) >= 0)
                && (_value.compareTo(BI_MAX_INTEGER) <= 0);
    }

    @Override
    protected boolean _inLongRange() {
        return (_value.compareTo(BI_MIN_LONG) >= 0)
                && (_value.compareTo(BI_MAX_LONG) <= 0);
    }

    /*
    /**********************************************************************
    /* Other overrides
    /**********************************************************************
     */
    
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
        if (o instanceof BigIntegerNode) {
            BigIntegerNode otherNode = (BigIntegerNode) o;
            return Objects.equals(otherNode._value, _value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_value);
    }
}

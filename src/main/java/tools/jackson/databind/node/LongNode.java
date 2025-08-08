package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.SerializationContext;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
public class LongNode
    extends NumericIntNode
{
    private static final long serialVersionUID = 3L;

    protected final long _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public LongNode(long v) { _value = v; }

    public static LongNode valueOf(long l) { return new LongNode(l); }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods
    /**********************************************************************
     */

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.LONG; }

    @Override
    public boolean isLong() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean() {
        return (_value != 0L);
    }

    @Override
    protected String _asString() {
        return String.valueOf(_value);
    }

    @Override
    public Number numberValue() {
        return Long.valueOf(_value);
    }

    @Override
    public short shortValue() {
        if (_inShortRange()) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("shortValue()");
    }

    @Override
    public short shortValue(short defaultValue) {
        return _inShortRange() ? (short) _value : defaultValue;
    }

    @Override
    public Optional<Short> shortValueOpt() {
        return _inShortRange() ? Optional.of((short) _value) : Optional.empty();
    }

    @Override
    public short asShort() {
        if (_inShortRange()) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("asShort()");
    }

    @Override
    public short asShort(short defaultValue) {
        return _inShortRange() ? (short) _value : defaultValue;
    }

    @Override
    public Optional<Short> asShortOpt() {
        return _inShortRange() ? Optional.of((short) _value) : Optional.empty();
    }

    @Override
    public int intValue() {
        if (_inIntRange()) {
            return (int) _value;
        }
        return _reportIntCoercionRangeFail("intValue()");
    }

    @Override
    public int intValue(int defaultValue) {
        return _inIntRange() ? (int) _value : defaultValue;
    }

    @Override
    public OptionalInt intValueOpt() {
        return _inIntRange() ? OptionalInt.of((int) _value) : OptionalInt.empty();
    }

    @Override
    public int asInt() {
        if (_inIntRange()) {
            return (int) _value;
        }
        return _reportIntCoercionRangeFail("asInt()");
    }

    @Override
    public int asInt(int defaultValue) {
        return _inIntRange() ? (int) _value : defaultValue;
    }

    @Override
    public OptionalInt asIntOpt() {
        return _inIntRange() ? OptionalInt.of((int) _value) : OptionalInt.empty();
    }

    @Override
    public long longValue() { return _value; }

    @Override
    public long longValue(long defaultValue) { return _value; }

    @Override
    public OptionalLong longValueOpt() {
        return OptionalLong.of(_value);
    }

    @Override
    public long asLong() { return _value; }

    @Override
    public long asLong(long defaultValue) { return _value; }

    @Override
    public OptionalLong asLongOpt() {
        return OptionalLong.of(_value);
    }

    @Override
    public BigInteger bigIntegerValue() { return BigInteger.valueOf(_value); }

    @Override
    public BigInteger bigIntegerValue(BigInteger defaultValue) {
        return BigInteger.valueOf(_value);
    }

    @Override
    public Optional<BigInteger> bigIntegerValueOpt() {
        return Optional.of(BigInteger.valueOf(_value));
    }

    @Override
    public BigDecimal decimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) { return decimalValue(); }

    @Override
    public Optional<BigDecimal> decimalValueOpt() { return Optional.of(decimalValue()); }

    /*
    /**********************************************************************
    /* Abstract methods impls for NumericIntNode
    /**********************************************************************
     */

    @Override
    protected int _asIntValueUnchecked() {
        return (int) _value;
    }

    @Override
    protected float _asFloatValueUnchecked() {
        return (float) _value;
    }

    @Override
    protected double _asDoubleValueUnchecked() {
        return (double) _value;
    }

    @Override
    protected boolean _inShortRange() {
        return (_value >= Short.MIN_VALUE && _value <= Short.MAX_VALUE);
    }

    @Override
    protected boolean _inIntRange() {
        return (_value >= Integer.MIN_VALUE) && (_value <= Integer.MAX_VALUE);
    }

    @Override
    protected boolean _inLongRange() { return true; }
    
    /*
    /**********************************************************************
    /* Overridden methods, other
    /**********************************************************************
     */

    @Override
    public final void serialize(JsonGenerator jg, SerializationContext provider)
        throws JacksonException
    {
        jg.writeNumber(_value);
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof LongNode) {
            return ((LongNode) o)._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((int) _value) ^ (int) (_value >> 32);
    }
}

package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.SerializationContext;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
public class LongNode
    extends NumericNode
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

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.LONG; }
    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isLong() { return true; }

    @Override
    public boolean isNaN() { return false; }

    @Override
    public boolean canConvertToInt() {
        return (_value >= Integer.MIN_VALUE && _value <= Integer.MAX_VALUE);
    }

    @Override public boolean canConvertToLong() { return true; }

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
        if (_value >= Short.MIN_VALUE && _value <= Short.MAX_VALUE) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("shortValue()");
    }

    @Override
    public int intValue() {
        if (canConvertToInt()) {
            return (int) _value;
        }
        return _reportIntCoercionRangeFail("intValue()");
    }

    @Override
    public int intValue(int defaultValue) {
        return canConvertToInt() ? (int) _value : defaultValue;
    }

    @Override
    public OptionalInt intValueOpt() {
        return canConvertToInt() ? OptionalInt.of((int) _value) : OptionalInt.empty();
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
    public BigDecimal decimalValue() { return BigDecimal.valueOf(_value); }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) { return decimalValue(); }

    @Override
    public Optional<BigDecimal> decimalValueOpt() { return Optional.of(decimalValue()); }

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

package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.*;
import tools.jackson.databind.SerializationContext;

/**
 * Numeric node that contains simple 32-bit integer values.
 */
public class IntNode
    extends NumericIntNode
{
    private static final long serialVersionUID = 3L;

    // // // Let's cache small set of common value

    final static int MIN_CANONICAL = -1;
    final static int MAX_CANONICAL = 10;

    private final static IntNode[] CANONICALS;
    static {
        int count = MAX_CANONICAL - MIN_CANONICAL + 1;
        CANONICALS = new IntNode[count];
        for (int i = 0; i < count; ++i) {
            CANONICALS[i] = new IntNode(MIN_CANONICAL + i);
        }
    }

    /**
     * Integer value this node contains
     */
    protected final int _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public IntNode(int v) { _value = v; }

    public static IntNode valueOf(int i) {
        if (i > MAX_CANONICAL || i < MIN_CANONICAL) return new IntNode(i);
        return CANONICALS[i - MIN_CANONICAL];
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods
    /**********************************************************************
     */

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.INT; }

    @Override
    public boolean isInt() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean() {
        return (_value != 0);
    }

    @Override
    protected String _asString() {
        return String.valueOf(_value);
    }
    
    @Override
    public Number numberValue() {
        return Integer.valueOf(_value);
    }

    @Override
    public short shortValue() {
        if (inShortRange()) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("shortValue()");
    }

    @Override
    public short shortValue(short defaultValue) {
        return inShortRange() ? (short) _value : defaultValue;
    }

    @Override
    public Optional<Short> shortValueOpt() {
        return inShortRange() ? Optional.of((short) _value) : Optional.empty();
    }

    @Override
    public short asShort() {
        if (inShortRange()) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("asShort()");
    }

    @Override
    public short asShort(short defaultValue) {
        return inShortRange() ? (short) _value : defaultValue;
    }

    @Override
    public Optional<Short> asShortOpt() {
        return inShortRange() ? Optional.of((short) _value) : Optional.empty();
    }

    @Override
    public int intValue() { return _value; }

    @Override
    public int intValue(int defaultValue) { return _value; }

    @Override
    public OptionalInt intValueOpt() {
        return OptionalInt.of(_value);
    }

    @Override
    public int asInt() {
        return _value;
    }

    @Override
    public int asInt(int defaultValue) {
        return _value;
    }

    @Override
    public OptionalInt asIntOpt() {
        return OptionalInt.of(_value);
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
    public short _asShortValueUnchecked() {
        return (short) _value;
    }

    @Override
    public long _asLongValueUnchecked() {
        return _value;
    }

    @Override
    public int _asIntValueUnchecked() {
        return _value;
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
    public boolean inShortRange() {
        return (_value >= Short.MIN_VALUE && _value <= Short.MAX_VALUE);
    }

    @Override
    public boolean inIntRange() {
        return true;
    }

    @Override
    public boolean inLongRange() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, other
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
        if (o instanceof IntNode otherNode) {
            return otherNode._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() { return _value; }
}

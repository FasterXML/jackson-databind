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
 * Numeric node that contains simple 32-bit integer values.
 */
public class IntNode
    extends NumericNode
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

    @Override public JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.INT; }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isInt() { return true; }

    @Override
    public boolean isNaN() { return false; }
    
    @Override public boolean canConvertToInt() { return true; }
    @Override public boolean canConvertToLong() { return true; }

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
        if (_value >= Short.MIN_VALUE && _value <= Short.MAX_VALUE) {
            return (short) _value;
        }
        return _reportShortCoercionRangeFail("shortValue()");
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
    public float floatValue() { return (float) _value; }

    @Override
    public double doubleValue() { return (double) _value; }

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
    /* Overridden JsonNode methods, other
    /**********************************************************************
     */
    
    @Override
    public final void serialize(JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        g.writeNumber(_value);
    }

    /*
    /**********************************************************************
    /* Overridden standard methods
    /**********************************************************************
     */
    
    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof IntNode) {
            return ((IntNode) o)._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() { return _value; }
}

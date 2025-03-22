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
 * Numeric node that contains simple 16-bit integer values.
 */
public class ShortNode
    extends NumericIntNode
{
    private static final long serialVersionUID = 3L;

    protected final short _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public ShortNode(short v) { _value = v; }

    public static ShortNode valueOf(short l) { return new ShortNode(l); }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override
    public JsonParser.NumberType numberType() {
        // No SHORT enum so
        return JsonParser.NumberType.INT;
    }
    
    @Override
    public boolean isShort() { return true; }

    @Override public boolean canConvertToInt() { return true; }
    @Override public boolean canConvertToLong() { return true; }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access
    /**********************************************************************
     */

    @Override
    protected Boolean _asBoolean() {
        return _value != 0;
    }

    @Override
    protected String _asString() {
        return String.valueOf(_value);
    }

    @Override
    public Number numberValue() {
        return Short.valueOf(_value);
    }

    @Override
    public short shortValue() { return _value; }

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
        if (o instanceof ShortNode) {
            return ((ShortNode) o)._value == _value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _value;
    }
}

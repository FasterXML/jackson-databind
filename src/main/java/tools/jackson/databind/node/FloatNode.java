package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.OptionalDouble;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.databind.SerializationContext;

/**
 * {@code JsonNode} implementation for efficiently containing 32-bit
 * `float` values.
 */
public class FloatNode
    extends NumericFPNode
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

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.FLOAT; }

    @Override
    public boolean isFloat() { return true; }

    @Override
    public boolean isNaN() {
        return NumberOutput.notFinite(_value);
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
    public float floatValue() {
        return _value;
    }

    @Override
    public float floatValue(float defaultValue) {
        return _value;
    }

    @Override
    public float asFloat() {
        return _value;
    }

    @Override
    public float asFloat(float defaultValue) {
        return _value;
    }

    @Override
    public double doubleValue() {
        return _value;
    }

    @Override
    public double doubleValue(double defaultValue) {
        return _value;
    }

    @Override
    public OptionalDouble doubleValueOpt() {
        return OptionalDouble.of(_value);
    }

    @Override
    public double asDouble() {
        return _value;
    }

    @Override
    public double asDouble(double defaultValue) {
        return _value;
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        return OptionalDouble.of(_value);
    }

    /*
    /**********************************************************************
    /* NumericFPNode abstract method impls
    /**********************************************************************
     */

    @Override
    protected short _asShortValueUnchecked() {
        return (short) _value;
    }

    @Override
    protected int _asIntValueUnchecked() {
        return (int) _value;
    }
    
    @Override
    protected long _asLongValueUnchecked() {
        return (long) _value;
    }

    @Override
    protected BigInteger _asBigIntegerValueUnchecked() {
        return BigDecimal.valueOf(_value).toBigInteger();
    }

    @Override
    protected BigDecimal _asDecimalValueUnchecked() {
        return BigDecimal.valueOf(_value);
    }

    @Override
    protected boolean _hasFractionalPart() { return _value != Math.round(_value); }

    @Override
    protected boolean _inShortRange() {
        return !isNaN() && (_value >= Short.MIN_VALUE) && (_value <= Short.MAX_VALUE);
    }

    @Override
    protected boolean _inIntRange() {
        return !isNaN() && (_value >= Integer.MIN_VALUE) && (_value <= Integer.MAX_VALUE);
    }

    @Override
    protected boolean _inLongRange() {
        return !isNaN() && (_value >= Long.MIN_VALUE) && (_value <= Long.MAX_VALUE);
    }

    /*
    /**********************************************************************
    /* Overrides, other
    /**********************************************************************
     */
    
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
}

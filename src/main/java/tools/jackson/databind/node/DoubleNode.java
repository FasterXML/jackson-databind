package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberOutput;
import tools.jackson.databind.SerializationContext;

/**
 * Numeric node that contains 64-bit ("double precision")
 * floating point values simple 32-bit integer values.
 */
public class DoubleNode
    extends NumericFPNode
{
    private static final long serialVersionUID = 3L;

    protected final double _value;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DoubleNode(double v) { _value = v; }

    public static DoubleNode valueOf(double v) { return new DoubleNode(v); }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, simple properties
    /**********************************************************************
     */

    @Override
    public JsonParser.NumberType numberType() { return JsonParser.NumberType.DOUBLE; }

    @Override
    public boolean isDouble() { return true; }

    @Override
    public boolean isNaN() {
        return NumberOutput.notFinite(_value);
    }

    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, non-numeric
    /**********************************************************************
     */

    @Override
    protected String _asString() {
        return String.valueOf(_value);
    }
    
    /*
    /**********************************************************************
    /* Overridden JsonNode methods, scalar access, numeric
    /**********************************************************************
     */

    @Override
    public Number numberValue() {
        return Double.valueOf(_value);
    }

    @Override
    public float floatValue() {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return f;
        }
        return _reportFloatCoercionRangeFail("floatValue()");
    }

    @Override
    public float floatValue(float defaultValue) {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return f;
        }
        return defaultValue;
    }

    @Override
    public Optional<Float> floatValueOpt() {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return Optional.of(f);
        }
        return Optional.empty();
    }

    @Override
    public float asFloat() {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return f;
        }
        return _reportFloatCoercionRangeFail("asFloat()");
    }

    @Override
    public float asFloat(float defaultValue) {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return f;
        }
        return defaultValue;
    }

    @Override
    public Optional<Float> asFloatOpt() {
        float f = (float) _value;
        if (Float.isFinite(f)) {
            return Optional.of(f);
        }
        return Optional.empty();
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
    protected boolean _hasFractionalPart() { return _value != Math.rint(_value); }

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
        throws JacksonException
    {
        g.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o instanceof DoubleNode) {
            // We must account for NaNs: NaN does not equal NaN, therefore we have
            // to use Double.compare().
            final double otherValue = ((DoubleNode) o)._value;
            return Double.compare(_value, otherValue) == 0;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        // same as hashCode Double.class uses
        long l = Double.doubleToLongBits(_value);
        return ((int) l) ^ (int) (l >> 32);
    }
}

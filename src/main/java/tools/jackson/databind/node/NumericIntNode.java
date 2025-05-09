package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalDouble;

import tools.jackson.core.JsonToken;

/**
 * Intermediate node class used for numeric nodes that contain
 * integral values: provides partial implementation of common
 * methods.
 */
public abstract class NumericIntNode extends NumericNode
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Partial implementation of basic metadata/type accessors
    /**********************************************************************
     */
    
    @Override
    public final JsonToken asToken() { return JsonToken.VALUE_NUMBER_INT; }

    @Override
    public final boolean isIntegralNumber() { return true; }

    @Override
    public final boolean isNaN() { return false; }

    @Override final
    public boolean canConvertToShort() {
        return _inShortRange();
    }

    @Override final
    public boolean canConvertToInt() {
        return _inIntRange();
    }

    @Override final
    public boolean canConvertToLong() {
        return _inLongRange();
    }

    /*
    /**********************************************************************
    /* Partial implementation of numeric accessors
    /**********************************************************************
     */


    // Sub-classes need to define this; but with that can implement other 5 methods

    @Override
    public abstract BigInteger bigIntegerValue();

    @Override
    public BigInteger bigIntegerValue(BigInteger defaultValue) {
        return bigIntegerValue();
    }

    @Override
    public Optional<BigInteger> bigIntegerValueOpt() {
        return Optional.of(bigIntegerValue());
    }

    @Override
    public BigInteger asBigInteger() {
        return bigIntegerValue();
    }

    @Override
    public BigInteger asBigInteger(BigInteger defaultValue) {
        return bigIntegerValue();
    }

    @Override
    public Optional<BigInteger> asBigIntegerOpt() {
        return bigIntegerValueOpt();
    }

    // Float and Double handling straight-forward for all Integral types except BigInteger
    // (which needs range checks and overrides these implementations)

    @Override
    public float floatValue() {
        return _asFloatValueUnchecked();
    }

    @Override
    public float floatValue(float defaultValue) {
        return _asFloatValueUnchecked();
    }

    @Override
    public float asFloat() {
        return _asFloatValueUnchecked();
    }

    @Override
    public float asFloat(float defaultValue) {
        return _asFloatValueUnchecked();
    }

    @Override
    public double doubleValue() {
        return _asDoubleValueUnchecked();
    }

    @Override
    public double doubleValue(double defaultValue) {
        return _asDoubleValueUnchecked();
    }

    @Override
    public OptionalDouble doubleValueOpt() {
        return OptionalDouble.of(_asDoubleValueUnchecked());
    }

    @Override
    public double asDouble() {
        return _asDoubleValueUnchecked();
    }

    @Override
    public double asDouble(double defaultValue) {
        return _asDoubleValueUnchecked();
    }

    @Override
    public OptionalDouble asDoubleOpt() {
        return OptionalDouble.of(_asDoubleValueUnchecked());
    }

    // Sub-classes need to define this; but with that can implement other 5 methods
    //
    // public BigDecimal decimalValue()

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) { return decimalValue(); }

    @Override
    public Optional<BigDecimal> decimalValueOpt() { return Optional.of(decimalValue()); }

    @Override
    public BigDecimal asDecimal() { return decimalValue(); }
    
    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) { return decimalValue(); }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        return decimalValueOpt();
    }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes
    /**********************************************************************
     */

    protected abstract int _asIntValueUnchecked();
    
    protected abstract float _asFloatValueUnchecked();

    protected abstract double _asDoubleValueUnchecked();

    protected abstract boolean _inShortRange();

    protected abstract boolean _inIntRange();

    protected abstract boolean _inLongRange();
}

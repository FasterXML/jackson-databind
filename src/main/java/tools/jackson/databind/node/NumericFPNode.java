package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import tools.jackson.core.JsonToken;

/**
 * Intermediate node class used for numeric nodes that contain
 * floating-point values: provides partial implementation of common
 * methods.
 */
public abstract class NumericFPNode extends NumericNode
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************************
    /* Partial implementation of basic metadata/type accessors
    /**********************************************************************
     */
    
    @Override
    public final JsonToken asToken() { return JsonToken.VALUE_NUMBER_FLOAT; }

    @Override
    public final boolean isFloatingPointNumber() { return true; }

    @Override
    public final boolean canConvertToShort() {
        return canConvertToExactIntegral() && _inShortRange();
    }

    @Override
    public final boolean canConvertToInt() {
        return canConvertToExactIntegral() && _inIntRange();
    }

    @Override
    public final boolean canConvertToLong() {
        return canConvertToExactIntegral() && _inLongRange();
    }

    @Override
    public final boolean canConvertToExactIntegral() {
        return !isNaN() && !_hasFractionalPart();
    }

    /*
    /**********************************************************************
    /* Partial implementation of numeric accessors
    /**********************************************************************
     */

    // // // Integer value accessors

    @Override
    public final short shortValue() {
        if (!_inShortRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("shortValue()");
            }
            return _reportShortCoercionRangeFail("shortValue()");
        }
        if (_hasFractionalPart()) {
            _reportShortCoercionFractionFail("shortValue()");
        }
        return _asShortValueUnchecked();
    }

    @Override
    public final short shortValue(short defaultValue) {
        if (!_inShortRange() || _hasFractionalPart()) {
            return defaultValue;
        }
        return _asShortValueUnchecked();
    }

    @Override
    public final Optional<Short> shortValueOpt() {
        if (!_inShortRange() || _hasFractionalPart()) {
            return Optional.empty();
        }
        return Optional.of(_asShortValueUnchecked());
    }

    @Override
    public short asShort() {
        if (!_inShortRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("asShort()");
            }
            return _reportShortCoercionRangeFail("asShort()");
        }
        return _asShortValueUnchecked();
    }

    @Override
    public short asShort(short defaultValue) {
        if (!_inShortRange()) {
            return defaultValue;
        }
        return _asShortValueUnchecked();
    }

    @Override
    public Optional<Short> asShortOpt() {
        if (!_inShortRange()) {
            return Optional.empty();
        }
        return Optional.of(_asShortValueUnchecked());
    }

    @Override
    public final int intValue() {
        if (!_inIntRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("intValue()");
            }
            return _reportIntCoercionRangeFail("intValue()");
        }
        if (_hasFractionalPart()) {
            _reportIntCoercionFractionFail("intValue()");
        }
        return _asIntValueUnchecked();
    }

    @Override
    public final int intValue(int defaultValue) {
        if (!_inIntRange() || _hasFractionalPart()) {
             return defaultValue;
        }
        return _asIntValueUnchecked();
    }

    @Override
    public final OptionalInt intValueOpt() {
        if (!_inIntRange() || _hasFractionalPart()) {
            return OptionalInt.empty();
       }
       return OptionalInt.of(_asIntValueUnchecked());
    }

    @Override
    public int asInt() {
        if (!_inIntRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("asInt()");
            }
            return _reportIntCoercionRangeFail("asInt()");
        }
        return _asIntValueUnchecked();
    }

    @Override
    public int asInt(int defaultValue) {
        if (!_inIntRange()) {
            return defaultValue;
        }
        return _asIntValueUnchecked();
    }

    @Override
    public OptionalInt asIntOpt() {
        if (!_inIntRange()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(_asIntValueUnchecked());
    }

    @Override
    public final long longValue() {
        if (!_inLongRange()) {
            if (isNaN()) {
                _reportLongCoercionNaNFail("longValue()");
            }
            return _reportLongCoercionRangeFail("longValue()");
        }
        if (_hasFractionalPart()) {
            _reportLongCoercionFractionFail("longValue()");
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final long longValue(long defaultValue) {
        if (!_inLongRange() || _hasFractionalPart()) {
            return defaultValue;
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final OptionalLong longValueOpt() {
        if (!_inLongRange() || _hasFractionalPart()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(_asLongValueUnchecked());
    }

    @Override
    public final long asLong() {
        if (!_inLongRange()) {
            if (isNaN()) {
                _reportLongCoercionNaNFail("asLong()");
            }
            return _reportLongCoercionRangeFail("asLong()");
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final long asLong(long defaultValue) {
        if (!_inLongRange()) {
            return defaultValue;
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final OptionalLong asLongOpt() {
        if (!_inLongRange()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(_asLongValueUnchecked());
    }

    @Override
    public final BigInteger bigIntegerValue() {
        if (isNaN()) {
            _reportBigIntegerCoercionNaNFail("bigIntegerValue()");
        }
        if (_hasFractionalPart()) {
            _reportBigIntegerCoercionFractionFail("bigIntegerValue()");
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final BigInteger bigIntegerValue(BigInteger defaultValue) {
        if (isNaN() || _hasFractionalPart()) {
            return defaultValue;
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final Optional<BigInteger> bigIntegerValueOpt() {
        if (isNaN() || _hasFractionalPart()) {
            return Optional.empty();
        }
        return Optional.of(_asBigIntegerValueUnchecked());
    }

    @Override
    public final BigInteger asBigInteger() {
        if (isNaN()) {
            _reportBigIntegerCoercionNaNFail("asBigInteger()");
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final BigInteger asBigInteger(BigInteger defaultValue) {
        if (isNaN()) {
            return defaultValue;
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final Optional<BigInteger> asBigIntegerOpt() {
        if (isNaN()) {
            return Optional.empty();
        }
        return Optional.of(_asBigIntegerValueUnchecked());
    }

    // // // FP value accessors
    
    @Override
    public BigDecimal decimalValue() {
        if (isNaN()) {
            _reportBigDecimalCoercionNaNFail("decimalValue()");
        }
        return _asDecimalValueUnchecked();
    }

    @Override
    public BigDecimal decimalValue(BigDecimal defaultValue) {
        if (isNaN()) {
            return defaultValue;
        }
        return _asDecimalValueUnchecked();
    }

    @Override
    public Optional<BigDecimal> decimalValueOpt() {
        if (isNaN()) {
            return Optional.empty();
        }
        return Optional.of(_asDecimalValueUnchecked());
    }

    @Override
    public BigDecimal asDecimal() {
        if (isNaN()) {
            _reportBigDecimalCoercionNaNFail("asDecimal()");
        }
        return _asDecimalValueUnchecked();
    }
    
    @Override
    public BigDecimal asDecimal(BigDecimal defaultValue) {
        if (isNaN()) {
            return defaultValue;
        }
        return _asDecimalValueUnchecked();
    }

    @Override
    public Optional<BigDecimal> asDecimalOpt() {
        if (isNaN()) {
            return Optional.empty();
        }
        return Optional.of(_asDecimalValueUnchecked());
    }

    /*
    /**********************************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************************
     */

    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a {@code short} without any checks (wrt NaN or value range),
     * so caller must ensure validity prior to calling
     */
    protected abstract short _asShortValueUnchecked();
    
    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a {@code int} without any checks (wrt NaN or value range),
     * so caller must ensure validity prior to calling
     */
    protected abstract int _asIntValueUnchecked();

    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a {@code long} without any checks (wrt NaN or value range),
     * so caller must ensure validity prior to calling
     */
    protected abstract long _asLongValueUnchecked();

    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a {@link BigInteger} without any checks (wrt NaN), so caller
     * must ensure validity prior to calling
     */
    protected abstract BigInteger _asBigIntegerValueUnchecked();

    // NOTE: we do not need these ones (not enough commonality):
    //protected abstract float _asFloatValueUnchecked();
    //protected abstract double _asDoubleValueUnchecked();

    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a {@link BigDecimal} without any checks (wrt NaN), so caller
     * must ensure validity prior to calling
     */
    protected abstract BigDecimal _asDecimalValueUnchecked();

    protected abstract boolean _hasFractionalPart();

    protected abstract boolean _inShortRange();

    protected abstract boolean _inIntRange();

    protected abstract boolean _inLongRange();
}

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
        return canConvertToExactIntegral() && inShortRange();
    }

    @Override
    public final boolean canConvertToInt() {
        return canConvertToExactIntegral() && inIntRange();
    }

    @Override
    public final boolean canConvertToLong() {
        return canConvertToExactIntegral() && inLongRange();
    }

    @Override
    public final boolean canConvertToExactIntegral() {
        return !isNaN() && !hasFractionalPart();
    }

    /*
    /**********************************************************************
    /* Partial implementation of numeric accessors
    /**********************************************************************
     */

    // // // Integer value accessors

    @Override
    public final short shortValue() {
        if (!inShortRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("shortValue()");
            }
            return _reportShortCoercionRangeFail("shortValue()");
        }
        if (hasFractionalPart()) {
            _reportShortCoercionFractionFail("shortValue()");
        }
        return _asShortValueUnchecked();
    }

    @Override
    public final short shortValue(short defaultValue) {
        if (!inShortRange() || hasFractionalPart()) {
            return defaultValue;
        }
        return _asShortValueUnchecked();
    }

    @Override
    public final Optional<Short> shortValueOpt() {
        if (!inShortRange() || hasFractionalPart()) {
            return Optional.empty();
        }
        return Optional.of(_asShortValueUnchecked());
    }

    @Override
    public short asShort() {
        if (!inShortRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("asShort()");
            }
            return _reportShortCoercionRangeFail("asShort()");
        }
        return _asShortValueUnchecked();
    }

    @Override
    public short asShort(short defaultValue) {
        if (!inShortRange()) {
            return defaultValue;
        }
        return _asShortValueUnchecked();
    }

    @Override
    public Optional<Short> asShortOpt() {
        if (!inShortRange()) {
            return Optional.empty();
        }
        return Optional.of(_asShortValueUnchecked());
    }

    @Override
    public final int intValue() {
        if (!inIntRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("intValue()");
            }
            return _reportIntCoercionRangeFail("intValue()");
        }
        if (hasFractionalPart()) {
            _reportIntCoercionFractionFail("intValue()");
        }
        return _asIntValueUnchecked();
    }

    @Override
    public final int intValue(int defaultValue) {
        if (!inIntRange() || hasFractionalPart()) {
             return defaultValue;
        }
        return _asIntValueUnchecked();
    }

    @Override
    public final OptionalInt intValueOpt() {
        if (!inIntRange() || hasFractionalPart()) {
            return OptionalInt.empty();
       }
       return OptionalInt.of(_asIntValueUnchecked());
    }

    @Override
    public int asInt() {
        if (!inIntRange()) {
            if (isNaN()) {
                _reportIntCoercionNaNFail("asInt()");
            }
            return _reportIntCoercionRangeFail("asInt()");
        }
        return _asIntValueUnchecked();
    }

    @Override
    public int asInt(int defaultValue) {
        if (!inIntRange()) {
            return defaultValue;
        }
        return _asIntValueUnchecked();
    }

    @Override
    public OptionalInt asIntOpt() {
        if (!inIntRange()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(_asIntValueUnchecked());
    }

    @Override
    public final long longValue() {
        if (!inLongRange()) {
            if (isNaN()) {
                _reportLongCoercionNaNFail("longValue()");
            }
            return _reportLongCoercionRangeFail("longValue()");
        }
        if (hasFractionalPart()) {
            _reportLongCoercionFractionFail("longValue()");
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final long longValue(long defaultValue) {
        if (!inLongRange() || hasFractionalPart()) {
            return defaultValue;
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final OptionalLong longValueOpt() {
        if (!inLongRange() || hasFractionalPart()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(_asLongValueUnchecked());
    }

    @Override
    public final long asLong() {
        if (!inLongRange()) {
            if (isNaN()) {
                _reportLongCoercionNaNFail("asLong()");
            }
            return _reportLongCoercionRangeFail("asLong()");
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final long asLong(long defaultValue) {
        if (!inLongRange()) {
            return defaultValue;
        }
        return _asLongValueUnchecked();
    }

    @Override
    public final OptionalLong asLongOpt() {
        if (!inLongRange()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(_asLongValueUnchecked());
    }

    @Override
    public final BigInteger bigIntegerValue() {
        if (isNaN()) {
            _reportBigIntegerCoercionNaNFail("bigIntegerValue()");
        }
        if (hasFractionalPart()) {
            _reportBigIntegerCoercionFractionFail("bigIntegerValue()");
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final BigInteger bigIntegerValue(BigInteger defaultValue) {
        if (isNaN() || hasFractionalPart()) {
            return defaultValue;
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final Optional<BigInteger> bigIntegerValueOpt() {
        if (isNaN() || hasFractionalPart()) {
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
}

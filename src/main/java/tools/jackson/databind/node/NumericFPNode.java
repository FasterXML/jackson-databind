package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

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
    public final BigInteger bigIntegerValue() {
        if (isNaN()) {
            _reportBigDecimalCoercionNaNFail("bigIntegerValue()");
        }
        if (_hasFractionalPart()) {
            _reportBigIntegerCoercionFractionFail("bigIntegerValue()");
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final BigInteger bigIntegerValue(BigInteger defaultValue) {
        if (_hasFractionalPart()) {
            return defaultValue;
        }
        return _asBigIntegerValueUnchecked();
    }

    @Override
    public final Optional<BigInteger> bigIntegerValueOpt() {
        if (_hasFractionalPart()) {
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
     * value as a BigInteger without any checks (wrt NaN), so caller
     * must ensure validity prior to calling
     */
    protected abstract BigInteger _asBigIntegerValueUnchecked();

    /**
     * Method for sub-classes to implement; returns the underlying
     * value as a BigDecimal without any checks (wrt NaN), so caller
     * must ensure validity prior to calling
     */
    protected abstract BigDecimal _asDecimalValueUnchecked();

    protected abstract boolean _hasFractionalPart();

    protected abstract boolean _inShortRange();

    protected abstract boolean _inIntRange();

    protected abstract boolean _inLongRange();
}

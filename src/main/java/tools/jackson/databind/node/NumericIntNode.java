package tools.jackson.databind.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

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

    /*
    /**********************************************************************
    /* Partial implementation of numeric accessors
    /**********************************************************************
     */

    // Sub-classes need to define this; but with that can implement other 5 methods
    //
    // public BigInteger bigIntegerValue()

    @Override
    public BigInteger bigIntegerValue(BigInteger defaultValue) {
        return bigIntegerValue();
    }

    @Override
    public Optional<BigInteger> bigIntegerValueOpt() {
        return Optional.of(bigIntegerValue());
    }

    /*
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
    */

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


}

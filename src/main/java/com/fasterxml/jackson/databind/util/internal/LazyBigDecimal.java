package com.fasterxml.jackson.databind.util.internal;

import com.fasterxml.jackson.core.io.NumberInput;

import java.math.BigDecimal;
import java.math.BigInteger;

public class LazyBigDecimal implements LazyNumber {
    private final String _value;
    private final boolean _useFastParser;

    public LazyBigDecimal(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    @Override
    public Number getNumber() {
        return NumberInput.parseBigDecimal(_value, _useFastParser);
    }
    }
}

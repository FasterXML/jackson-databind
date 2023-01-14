package com.fasterxml.jackson.databind.util.internal;

import com.fasterxml.jackson.core.io.NumberInput;

import java.math.BigDecimal;

public class LazyBigDecimal implements LazyNumber {
    private final String _value;
    private final boolean _useFastParser;
    private BigDecimal _decimal;

    public LazyBigDecimal(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    @Override
    public Number getNumber() {
        if (_decimal == null) {
            _decimal = NumberInput.parseBigDecimal(_value, _useFastParser);;
        }
        return _decimal;
    }

    @Override
    public String getText() {
        return _value;
    }
}

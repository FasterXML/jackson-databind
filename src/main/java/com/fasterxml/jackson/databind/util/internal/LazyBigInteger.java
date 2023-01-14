package com.fasterxml.jackson.databind.util.internal;

import com.fasterxml.jackson.core.io.NumberInput;

import java.math.BigInteger;

public class LazyBigInteger implements LazyNumber {
    private final String _value;
    private final boolean _useFastParser;
    private BigInteger _integer;


    public LazyBigInteger(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    @Override
    public Number getNumber() {
        if (_integer == null) {
            _integer = NumberInput.parseBigInteger(_value, _useFastParser);
        }
        return _integer;
    }

    @Override
    public String getText() {
        return _value;
    }
}

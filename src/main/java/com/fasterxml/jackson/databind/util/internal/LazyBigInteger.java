package com.fasterxml.jackson.databind.util.internal;

import com.fasterxml.jackson.core.io.NumberInput;

public class LazyBigInteger implements LazyNumber {
    private final String _value;
    private final boolean _useFastParser;

    public LazyBigInteger(final String value, final boolean useFastParser) {
        this._value = value;
        this._useFastParser = useFastParser;
    }

    @Override
    public Number getNumber() {
        return NumberInput.parseBigInteger(_value, _useFastParser);
    }

    @Override
    public String getText() {
        return _value;
    }
}

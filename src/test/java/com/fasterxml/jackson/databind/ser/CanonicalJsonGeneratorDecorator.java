package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGeneratorDecorator;

public class CanonicalJsonGeneratorDecorator implements JsonGeneratorDecorator {

    private ValueToString<BigDecimal> _serializer;

    public CanonicalJsonGeneratorDecorator(ValueToString<BigDecimal> serializer) {
        this._serializer = serializer;
    }

    @Override
    public JsonGenerator decorate(JsonFactory factory, JsonGenerator generator) {
        return new CanonicalNumberGenerator(generator, _serializer);
    }
}

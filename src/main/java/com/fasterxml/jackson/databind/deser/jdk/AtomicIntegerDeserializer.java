package com.fasterxml.jackson.databind.deser.jdk;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

public class AtomicIntegerDeserializer extends StdScalarDeserializer<AtomicInteger>
{
    public AtomicIntegerDeserializer() { super(AtomicInteger.class); }

    @Override
    public AtomicInteger deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.isExpectedNumberIntToken()) {
            return new AtomicInteger(p.getIntValue());
        }
        // 12-Jun-2020, tatu: May look convoluted, but need to work correctly with
        //   CoercionConfig
        Integer I = _parseInteger(p, ctxt, AtomicInteger.class);
        return (I == null) ? null : new AtomicInteger(I.intValue());
    }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new AtomicInteger();
    }
}

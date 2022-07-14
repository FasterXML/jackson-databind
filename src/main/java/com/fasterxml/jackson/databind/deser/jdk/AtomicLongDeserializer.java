package com.fasterxml.jackson.databind.deser.jdk;

import java.util.concurrent.atomic.AtomicLong;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

public class AtomicLongDeserializer extends StdScalarDeserializer<AtomicLong>
{
    public AtomicLongDeserializer() { super(AtomicLong.class); }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override // @since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new AtomicLong();
    }

    @Override
    public AtomicLong deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (p.isExpectedNumberIntToken()) {
            return new AtomicLong(p.getLongValue());
        }
        // 12-Jun-2020, tatu: May look convoluted, but need to work correctly with
        //   CoercionConfig
        Long L = _parseLong(p, ctxt, AtomicLong.class);
        return (L == null) ? null : new AtomicLong(L.intValue());
    }
}

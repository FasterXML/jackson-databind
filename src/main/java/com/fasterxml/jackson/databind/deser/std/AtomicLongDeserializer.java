package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.LogicalType;

// @since 2.12
public class AtomicLongDeserializer extends StdScalarDeserializer<AtomicLong>
{
    private static final long serialVersionUID = 1L;

    public AtomicLongDeserializer() { super(AtomicLong.class); }

    @Override
    public AtomicLong deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.isExpectedNumberIntToken()) {
            return new AtomicLong(p.getLongValue());
        }
        // 12-Jun-2020, tatu: May look convoluted, but need to work correctly with
        //   CoercionConfig
        Long L = _parseLong(p, ctxt, AtomicLong.class);
        return (L == null) ? null : new AtomicLong(L.intValue());
    }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override // @since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return new AtomicLong();
    }
}

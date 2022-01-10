package com.fasterxml.jackson.databind.deser.jdk;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

public class AtomicBooleanDeserializer extends StdScalarDeserializer<AtomicBoolean>
{
    public AtomicBooleanDeserializer() { super(AtomicBoolean.class); }

    @Override
    public LogicalType logicalType() { return LogicalType.Boolean; }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new AtomicBoolean(false);
    }

    @Override
    public AtomicBoolean deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_TRUE) {
            return new AtomicBoolean(true);
        }
        if (t == JsonToken.VALUE_FALSE) {
            return new AtomicBoolean(false);
        }
        // 12-Jun-2020, tatu: May look convoluted, but need to work correctly with
        //   CoercionConfig
        Boolean b = _parseBoolean(p, ctxt, AtomicBoolean.class);
        return (b == null) ? null : new AtomicBoolean(b.booleanValue());
    }
}

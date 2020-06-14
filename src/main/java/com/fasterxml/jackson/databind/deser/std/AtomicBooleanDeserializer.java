package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class AtomicBooleanDeserializer extends StdScalarDeserializer<AtomicBoolean>
{
    private static final long serialVersionUID = 1L;

    public AtomicBooleanDeserializer() { super(AtomicBoolean.class); }

    @Override
    public AtomicBoolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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

    @Override
    public LogicalType logicalType() { return LogicalType.Boolean; }

    @Override // @since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return new AtomicBoolean(false);
    }
}

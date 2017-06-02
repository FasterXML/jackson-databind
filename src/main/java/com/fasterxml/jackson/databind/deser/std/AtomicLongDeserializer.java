package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongDeserializer extends JsonDeserializer<AtomicLong> {

    private static final long serialVersionUID = -1L;

    @Override
    public AtomicLong deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new AtomicLong(p.getLongValue());
    }
}

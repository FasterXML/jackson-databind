package com.fasterxml.jackson.databind.deser.std;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerDeserializer extends JsonDeserializer<AtomicInteger> {

    private static final long serialVersionUID = -1L;

    @Override
    public AtomicInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return new AtomicInteger(p.getIntValue());
    }
}

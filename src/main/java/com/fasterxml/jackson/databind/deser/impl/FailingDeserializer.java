package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Special bogus "serializer" that will throw
 * {@link com.fasterxml.jackson.databind.exc.MismatchedInputException} if an attempt is made to deserialize
 * a value. This is used as placeholder to avoid NPEs for uninitialized
 * structured serializers or handlers.
 */
public class FailingDeserializer extends StdDeserializer<Object>
{
    private static final long serialVersionUID = 1L;

    protected final String _message;

    public FailingDeserializer(String m) {
        this(Object.class, m);
    }

    // @since 2.12
    public FailingDeserializer(Class<?> rawType, String m) {
        super(rawType);
        _message = m;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ctxt.reportInputMismatch(this, _message);
        return null;
    }
}

package com.fasterxml.jackson.databind.deser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Special bogus "serializer" that will throw
 * {@link com.fasterxml.jackson.databind.exc.MismatchedInputException}
 * if an attempt is made to deserialize a value.
 * This is used for "known unknown" types: types that we can recognize
 * but can not support easily (or support known to be added via extension
 * module).
 */
public class UnsupportedTypeDeserializer extends StdDeserializer<Object>
{
    protected final JavaType _type;

    protected final String _message;

    public UnsupportedTypeDeserializer(JavaType t, String m) {
        super(t);
        _type = t;
        _message = m;
    }
    
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ctxt.reportBadDefinition(_type, _message);
        return null;
    }
}

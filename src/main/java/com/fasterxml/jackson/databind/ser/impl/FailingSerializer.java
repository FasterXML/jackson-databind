package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Special bogus "serializer" that will throw
 * {@link JsonGenerationException} if its {@link #serialize}
 * gets invoked. Most commonly registered as handler for unknown types,
 * as well as for catching unintended usage (like trying to use null
 * as Map/Object key).
 */
public final class FailingSerializer
    extends StdSerializer<Object>
{
    final String _msg;
    
    public FailingSerializer(String msg) {
        super(Object.class);
        _msg = msg;
    }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        throw new JsonGenerationException(_msg);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        ;
    }
}

package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * Simple deserializer for handling {@link java.util.Date} values.
 *<p>
 * One way to customize Date formats accepted is to override method
 * {@link DeserializationContext#parseDate} that this basic
 * deserializer calls.
 */
public class DateDeserializer
    extends StdScalarDeserializer<Date>
{
    public DateDeserializer() { super(Date.class); }
    
    @Override
    public java.util.Date deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return _parseDate(jp, ctxt);
    }
}

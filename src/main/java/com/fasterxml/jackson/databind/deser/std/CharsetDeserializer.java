package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.DeserializationContext;

// [JACKSON-789]
public class CharsetDeserializer
    extends FromStringDeserializer<Charset>
{
    private static final long serialVersionUID = 1L;

    public final static CharsetDeserializer instance = new CharsetDeserializer();

    public CharsetDeserializer() { super(Charset.class); }

    @Override
    protected Charset _deserialize(String value, DeserializationContext ctxt)
        throws IOException
    {
        return Charset.forName(value);
    }
}
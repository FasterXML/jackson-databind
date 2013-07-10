package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.net.InetAddress;

import com.fasterxml.jackson.databind.DeserializationContext;

/**
 * As per [JACKSON-484], also need special handling for InetAddress...
 */
class InetAddressDeserializer
    extends FromStringDeserializer<InetAddress>
{
    private static final long serialVersionUID = 1L;

    public final static InetAddressDeserializer instance = new InetAddressDeserializer();

    public InetAddressDeserializer() { super(InetAddress.class); }

    @Override
    protected InetAddress _deserialize(String value, DeserializationContext ctxt)
        throws IOException
    {
        return InetAddress.getByName(value);
    }
}
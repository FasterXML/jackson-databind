package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.net.InetAddress;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Simple serializer for {@link java.net.InetAddress}. Main complexity is
 * with registration, since same serializer is to be used for sub-classes.
 */
@SuppressWarnings("serial")
public class InetAddressSerializer
    extends StdScalarSerializer<InetAddress>
{
    public InetAddressSerializer() { super(InetAddress.class); }

    @Override
    public void serialize(InetAddress value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        // [databind#1605] ignoring hostname; address is required to avoid DNS lookup upon deserialization
        String str = value.getHostAddress();
        jgen.writeString(str);
    }

    @Override
    public void serializeWithType(InetAddress value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonGenerationException
    {
        // Better ensure we don't use specific sub-classes...
        typeSer.writeTypePrefixForScalar(value, jgen, InetAddress.class);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }
}

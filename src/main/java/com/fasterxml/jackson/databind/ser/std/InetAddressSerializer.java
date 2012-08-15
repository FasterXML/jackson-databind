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
public class InetAddressSerializer
    extends StdScalarSerializer<InetAddress>
{
    public final static InetAddressSerializer instance = new InetAddressSerializer();
    
    public InetAddressSerializer() { super(InetAddress.class); }

    @Override
    public void serialize(InetAddress value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // Ok: get textual description; choose "more specific" part
        String str = value.toString().trim();
        int ix = str.indexOf('/');
        if (ix >= 0) {
            if (ix == 0) { // missing host name; use address
                str = str.substring(1);
            } else { // otherwise use name
                str = str.substring(0, ix);
            }
        }
        jgen.writeString(str);
    }

    @Override
    public void serializeWithType(InetAddress value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        // Better ensure we don't use specific sub-classes...
        typeSer.writeTypePrefixForScalar(value, jgen, InetAddress.class);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }
}

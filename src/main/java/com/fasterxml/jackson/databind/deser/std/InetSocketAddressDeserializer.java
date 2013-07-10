package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

/**
 * Deserializer for {@link InetSocketAddress}.
 *
 * @see <a href="https://github.com/FasterXML/jackson-databind/issues/48">Issue 48</a>
 */
public class InetSocketAddressDeserializer
    extends FromStringDeserializer<InetSocketAddress>
{
    private static final long serialVersionUID = 1L;

    public final static InetSocketAddressDeserializer instance = new InetSocketAddressDeserializer();

    public InetSocketAddressDeserializer() { super(InetSocketAddress.class); }

    @Override
    protected InetSocketAddress _deserialize(String value, DeserializationContext ctxt)
            throws IOException
    {
        if (value.startsWith("[")) {
            // bracketed IPv6 (with port number)

            int i = value.lastIndexOf(']');
            if (i == -1) {
                throw new InvalidFormatException(
                        "Bracketed IPv6 address must contain closing bracket.",
                        value, InetSocketAddress.class);
            }

            int j = value.indexOf(':', i);
            int port = j > -1 ? Integer.parseInt(value.substring(j + 1)) : 0;
            return new InetSocketAddress(value.substring(0, i + 1), port);
        } else {
            int i = value.indexOf(':');
            if (i != -1 && value.indexOf(':', i + 1) == -1) {
                // host:port
                int port = Integer.parseInt(value.substring(i));
                return new InetSocketAddress(value.substring(0, i), port);
            } else {
                // host or unbracketed IPv6, without port number
                return new InetSocketAddress(value, 0);
            }
        }
    }
}
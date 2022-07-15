package tools.jackson.databind.ser.jdk;

import java.net.*;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * Simple serializer for {@link InetSocketAddress}.
 */
public class InetSocketAddressSerializer
    extends StdScalarSerializer<InetSocketAddress>
{
    public InetSocketAddressSerializer() { super(InetSocketAddress.class); }

    @Override
    public void serialize(InetSocketAddress value, JsonGenerator jgen, SerializerProvider provider)
        throws JacksonException
    {
        InetAddress addr = value.getAddress();
        String str = addr == null ? value.getHostName() : addr.toString().trim();
        int ix = str.indexOf('/');
        if (ix >= 0) {
            if (ix == 0) { // missing host name; use address
                str = addr instanceof Inet6Address
                        ? "[" + str.substring(1) + "]" // bracket IPv6 addresses with
                        : str.substring(1);

            } else { // otherwise use name
                str = str.substring(0, ix);
            }
        }

        jgen.writeString(str + ":" + value.getPort());
    }

    @Override
    public void serializeWithType(InetSocketAddress value, JsonGenerator g,
            SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        // Better ensure we don't use specific sub-classes...
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, InetSocketAddress.class, JsonToken.VALUE_STRING));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }
}

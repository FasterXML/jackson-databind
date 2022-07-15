package tools.jackson.databind.ser.jdk;

import java.net.InetAddress;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * Simple serializer for {@link java.net.InetAddress}. Main complexity is
 * with registration, since same serializer is to be used for sub-classes.
 *<p>
 * Since 2.9 allows use of {@link JsonFormat} configuration (annotation,
 * per-type defaulting) so that if <code>JsonFormat.Shape.NUMBER</code>
 * (or <code>ARRAY</code>) is used, will serialize as "host address"
 * (dotted numbers) instead of simple conversion.
 */
public class InetAddressSerializer
    extends StdScalarSerializer<InetAddress>
{
    protected final boolean _asNumeric;

    public InetAddressSerializer() {
        this(false);
    }

    public InetAddressSerializer(boolean asNumeric) {
        super(InetAddress.class);
        _asNumeric = asNumeric;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        boolean asNumeric = false;
        if (format != null) {
            JsonFormat.Shape shape = format.getShape();
            if (shape.isNumeric() || shape == JsonFormat.Shape.ARRAY) {
                asNumeric = true;
            }
        }
        if (asNumeric != _asNumeric) {
            return new InetAddressSerializer(asNumeric);
        }
        return this;
    }

    @Override
    public void serialize(InetAddress value, JsonGenerator g, SerializerProvider provider)
        throws JacksonException
    {
        String str;

        if (_asNumeric) { // since 2.9
            str = value.getHostAddress();
        } else {
            // Ok: get textual description; choose "more specific" part
            str = value.toString().trim();
            int ix = str.indexOf('/');
            if (ix >= 0) {
                if (ix == 0) { // missing host name; use address
                    str = str.substring(1);
                } else { // otherwise use name
                    str = str.substring(0, ix);
                }
            }
        }
        g.writeString(str);
    }

    @Override
    public void serializeWithType(InetAddress value, JsonGenerator g,
            SerializerProvider ctxt, TypeSerializer typeSer) throws JacksonException
    {
        // Better ensure we don't use specific sub-classes...
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, InetAddress.class, JsonToken.VALUE_STRING));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }
}

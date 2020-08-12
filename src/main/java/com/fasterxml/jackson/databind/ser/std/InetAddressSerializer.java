package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.net.InetAddress;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Simple serializer for {@link java.net.InetAddress}. Main complexity is
 * with registration, since same serializer is to be used for sub-classes.
 *<p>
 * Since 2.9 allows use of {@link JsonFormat} configuration (annotation,
 * per-type defaulting) so that if <code>JsonFormat.Shape.NUMBER</code>
 * (or <code>ARRAY</code>) is used, will serialize as "host address"
 * (dotted numbers) instead of simple conversion.
 */
@SuppressWarnings("serial")
public class InetAddressSerializer
    extends StdScalarSerializer<InetAddress>
    implements ContextualSerializer
{
    /**
     * @since 2.9
     */
    protected final boolean _asNumeric;

    public InetAddressSerializer() {
        this(false);
    }

    /**
     * @since 2.9
     */
    public InetAddressSerializer(boolean asNumeric) {
        super(InetAddress.class);
        _asNumeric = asNumeric;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property) throws JsonMappingException
    {
        JsonFormat.Value format = findFormatOverrides(serializers,
                property, handledType());
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
    public void serialize(InetAddress value, JsonGenerator g, SerializerProvider provider) throws IOException
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
            SerializerProvider provider, TypeSerializer typeSer) throws IOException
    {
        // Better ensure we don't use specific sub-classes...
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, InetAddress.class, JsonToken.VALUE_STRING));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }
}

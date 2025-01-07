package tools.jackson.databind.ser.jdk;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Unlike other integral number array serializers, we do not just print out byte values
 * as numbers. Instead, we assume that it would make more sense to output content
 * as base64 encoded bytes (using default base64 encoding).
 *<p>
 * NOTE: since it is NOT serialized as an array, cannot use AsArraySerializer as base
 *<p>
 * NOTE: since 2.6, has been a main-level class; earlier was embedded in
 * {@link JDKArraySerializers}.
 */
@JacksonStdImpl
public class ByteArraySerializer extends StdSerializer<byte[]>
{
    public ByteArraySerializer() {
        super(byte[].class);
    }

    @Override
    public boolean isEmpty(SerializationContext prov, byte[] value) {
        return value.length == 0;
    }

    @Override
    public void serialize(byte[] value, JsonGenerator g, SerializationContext provider)
        throws JacksonException
    {
        g.writeBinary(provider.getConfig().getBase64Variant(),
                value, 0, value.length);
    }

    @Override
    public void serializeWithType(byte[] value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // most likely scalar
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
        g.writeBinary(ctxt.getConfig().getBase64Variant(),
                value, 0, value.length);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);

        /* OLD impl
        typeSer.writeTypePrefixForScalar(value, g);
        g.writeBinary(provider.getConfig().getBase64Variant(),
                value, 0, value.length);
        typeSer.writeTypeSuffixForScalar(value, g);
        */
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        acceptJsonFormatVisitorForBinary(visitor, typeHint);
    }
}

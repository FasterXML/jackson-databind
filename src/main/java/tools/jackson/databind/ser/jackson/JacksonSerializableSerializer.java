package tools.jackson.databind.ser.jackson;

import tools.jackson.core.*;
import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Generic handler for types that implement {@link JacksonSerializable}.
 *<p>
 * Note: given that this is used for anything that implements
 * interface, cannot be checked for direct class equivalence.
 *<p>
 * NOTE: in Jackson 2.x was named {@code JsonSerializableSerializer}
 */
@JacksonStdImpl
public class JacksonSerializableSerializer
    extends StdSerializer<JacksonSerializable>
{
    public final static JacksonSerializableSerializer instance = new JacksonSerializableSerializer();

    protected JacksonSerializableSerializer() { super(JacksonSerializable.class); }

    @Override
    public boolean isEmpty(SerializerProvider serializers, JacksonSerializable value) {
        if (value instanceof JacksonSerializable.Base) {
            return ((JacksonSerializable.Base) value).isEmpty(serializers);
        }
        return false;
    }

    @Override
    public void serialize(JacksonSerializable value, JsonGenerator gen, SerializerProvider serializers)
        throws JacksonException
    {
        value.serialize(gen, serializers);
    }

    @Override
    public final void serializeWithType(JacksonSerializable value, JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer) throws JacksonException
    {
        value.serializeWithType(gen, serializers, typeSer);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitor.expectAnyFormat(typeHint);
    }
}

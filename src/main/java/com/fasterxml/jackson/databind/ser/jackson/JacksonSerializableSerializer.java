package com.fasterxml.jackson.databind.ser.jackson;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JacksonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

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

package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Simple "bogus" serializer that will just serialize an empty
 * Object for any given value.
 * Quite similar to {@code UnknownSerializer} with the exception that
 * serialization never fails.
 */
@JacksonStdImpl
public class ToEmptyObjectSerializer
    extends StdSerializer<Object>
{
    protected ToEmptyObjectSerializer(Class<?> raw) {
        super(raw);
    }

    public ToEmptyObjectSerializer(JavaType type) {
        super(type);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider ctxt)
        throws JacksonException
    {
        gen.writeStartObject(value, 0);
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen,
            SerializerProvider ctxt, TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, ctxt,
                typeSer.typeId(value, JsonToken.START_OBJECT));
        typeSer.writeTypeSuffix(gen, ctxt, typeIdDef);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value) {
        return true;
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor,
            JavaType typeHint)
        throws JacksonException
    {
        /*JsonObjectFormatVisitor v =*/ visitor.expectObjectFormat(typeHint);
    }
}

package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonschema.visitors.JsonFormatVisitor;

/**
 * Serializer used for primitive boolean, as well as java.util.Boolean
 * wrapper type.
 *<p>
 * Since this is one of "native" types, no type information is ever
 * included on serialization (unlike for most scalar types as of 1.5)
 */
@JacksonStdImpl
public final class BooleanSerializer
    extends NonTypedScalarSerializerBase<Boolean>
{
    /**
     * Whether type serialized is primitive (boolean) or wrapper
     * (java.lang.Boolean); if true, former, if false, latter.
     */
    final boolean _forPrimitive;

    public BooleanSerializer(boolean forPrimitive)
    {
        super(Boolean.class);
        _forPrimitive = forPrimitive;
    }

    @Override
    public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeBoolean(value.booleanValue());
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitor visitor, JavaType typeHint)
    {
        visitor.booleanFormat();
    }
}
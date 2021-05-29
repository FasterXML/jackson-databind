package com.fasterxml.jackson.databind.deser.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support deserialization of {@link TokenBuffer}.
 *<p>
 * Note that we use scalar deserializer base just because we claim
 * to be of scalar for type information inclusion purposes; actual
 * underlying content can be of any (Object, Array, scalar) type.
 */
@JacksonStdImpl
public class TokenBufferDeserializer extends StdScalarDeserializer<TokenBuffer>
{
    public TokenBufferDeserializer() { super(TokenBuffer.class); }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Untyped;
    }

    @Override
    public TokenBuffer deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        return ctxt.bufferForInputBuffering(p).deserialize(p, ctxt);
    }
}

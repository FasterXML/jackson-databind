package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support deserialization of
 * {@link TokenBuffer}.
 *<p>
 * Note that we use scalar deserializer base just because we claim
 * to be of scalar for type information inclusion purposes; actual
 * underlying content can be of any (Object, Array, scalar) type.
 *
 * @since 1.9 (moved from higher-level package)
 */
@JacksonStdImpl
public class TokenBufferDeserializer
    extends StdScalarDeserializer<TokenBuffer>
{
    public TokenBufferDeserializer() { super(TokenBuffer.class); }

    @Override
    public TokenBuffer deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        TokenBuffer tb = new TokenBuffer(jp.getCodec());
        // quite simple, given that TokenBuffer is a JsonGenerator:
        tb.copyCurrentStructure(jp);
        return tb;
    }
}

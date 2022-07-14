package tools.jackson.databind.deser.jackson;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.TokenBuffer;

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

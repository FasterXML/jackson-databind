package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support serialization of {@link TokenBuffer};
 * and since it is part of core package, it can not implement
 * {@link com.fasterxml.jackson.databind.JsonSerializable}
 * (which is only included in the mapper package)
 */
@JacksonStdImpl
public class TokenBufferSerializer
    extends StdSerializer<TokenBuffer>
{
    public TokenBufferSerializer() { super(TokenBuffer.class); }

    @Override
    public void serialize(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        value.serialize(jgen);
    }

    /**
     * Implementing typed output for contents of a TokenBuffer is very tricky,
     * since we do not know for sure what its contents might look like (or, rather,
     * we do know when serializing, but not necessarily when deserializing!)
     * One possibility would be to check the current token, and use that to
     * determine if we would output JSON Array, Object or scalar value.
     * Jackson 1.5 did NOT include any type information; but this seems wrong,
     * and so 1.6 WILL include type information.
     *<p>
     * Note that we just claim it is scalar; this should work ok and is simpler
     * than doing introspection on both serialization and deserialization.
     */
    @Override
    public final void serializeWithType(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonGenerationException
    {
        typeSer.writeTypePrefixForScalar(value, jgen);
        serialize(value, jgen, provider);
        typeSer.writeTypeSuffixForScalar(value, jgen);
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        /* 01-Jan-2010, tatu: Not 100% sure what we should say here:
         *   type is basically not known. This seems closest
         *   approximation
         */
        return createSchemaNode("any", true);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        /* 01-Jan-2010, tatu: Not 100% sure what we should say here:
         *   type is basically not known. This seems closest
         *   approximation
         */
    	visitor.expectAnyFormat(typeHint);
    }
}    

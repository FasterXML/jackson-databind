package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support serialization of {@link TokenBuffer};
 * and since it is part of core package, it cannot implement
 * {@link com.fasterxml.jackson.databind.JsonSerializable}
 * (which is only included in the mapper package)
 */
@JacksonStdImpl
@SuppressWarnings("serial")
public class TokenBufferSerializer
    extends StdSerializer<TokenBuffer>
{
    public TokenBufferSerializer() { super(TokenBuffer.class); }

    @Override
    public void serialize(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        value.serialize(jgen);
    }

    /**
     * Implementing typed output for contents of a TokenBuffer is very tricky,
     * since we do not know for sure what its contents might look like (or, rather,
     * we do know when serializing, but not necessarily when deserializing!)
     * One possibility would be to check the current token, and use that to
     * determine if we would output JSON Array, Object or scalar value.
     *<p>
     * Note that we just claim it is scalar; this should work ok and is simpler
     * than doing introspection on both serialization and deserialization.
     */
    @Override
    public final void serializeWithType(TokenBuffer value, JsonGenerator g,
            SerializerProvider provider, TypeSerializer typeSer) throws IOException
    {
        // 28-Jun-2017, tatu: As per javadoc, not sure what to report as likely shape. Could
        //    even look into first actual token inside... but, for now let's keep it simple
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
        serialize(value, g, provider);
        typeSer.writeTypeSuffix(g, typeIdDef);
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        // Not 100% sure what we should say here: type is basically not known.
        // This seems like closest approximation
        return createSchemaNode("any", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        // Not 100% sure what we should say here: type is basically not known.
        // This seems like closest approximation
        visitor.expectAnyFormat(typeHint);
    }
}

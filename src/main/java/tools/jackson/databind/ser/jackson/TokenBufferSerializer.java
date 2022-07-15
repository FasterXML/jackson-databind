package tools.jackson.databind.ser.jackson;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.util.TokenBuffer;

/**
 * We also want to directly support serialization of {@link TokenBuffer};
 * and since it is part of core package, it cannot implement
 * {@link tools.jackson.databind.JacksonSerializable}
 * (which is only included in the mapper package)
 */
@JacksonStdImpl
public class TokenBufferSerializer
    extends StdSerializer<TokenBuffer>
{
    public TokenBufferSerializer() { super(TokenBuffer.class); }

    @Override
    public void serialize(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider)
        throws JacksonException
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
    public final void serializeWithType(TokenBuffer value, JsonGenerator g, SerializerProvider ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        // 28-Jun-2017, tatu: As per javadoc, not sure what to report as likely shape. Could
        //    even look into first actual token inside... but, for now let's keep it simple
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.VALUE_EMBEDDED_OBJECT));
        serialize(value, g, ctxt);
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        // Not 100% sure what we should say here: type is basically not known.
        // This seems like closest approximation
        visitor.expectAnyFormat(typeHint);
    }
}    

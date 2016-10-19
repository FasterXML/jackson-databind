package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Serializer used for primitive boolean, as well as java.util.Boolean
 * wrapper type.
 *<p>
 * Since this is one of "natural" (aka "native") types, no type information is ever
 * included on serialization (unlike for most other scalar types)
 */
@JacksonStdImpl
public final class BooleanSerializer
//In 2.9, removed use of intermediate type `NonTypedScalarSerializerBase`
    extends StdScalarSerializer<Object>
{
    private static final long serialVersionUID = 1L;

    /**
     * Whether type serialized is primitive (boolean) or wrapper
     * (java.lang.Boolean); if true, former, if false, latter.
     */
    protected final boolean _forPrimitive;

    public BooleanSerializer(boolean forPrimitive) {
        super(forPrimitive ? Boolean.TYPE : Boolean.class, false);
        _forPrimitive = forPrimitive;
    }

    @Override
    public void serialize(Object value, JsonGenerator g, SerializerProvider provider) throws IOException {
        g.writeBoolean(((Boolean) value).booleanValue());
    }

    @Override
    public final void serializeWithType(Object value, JsonGenerator g, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException
    {
        g.writeBoolean(((Boolean) value).booleanValue());
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("boolean", !_forPrimitive);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) throws JsonMappingException
    {
        visitor.expectBooleanFormat(typeHint);
    }
}

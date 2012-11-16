package com.fasterxml.jackson.databind.ser.std;

import java.lang.reflect.Type;
import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;

/**
 * This is a simple dummy serializer that will just output literal
 * JSON null value whenever serialization is requested.
 * Used as the default "null serializer" (which is used for serializing
 * null object references unless overridden), as well as for some
 * more exotic types (java.lang.Void).
 */
@JacksonStdImpl
public class NullSerializer
    extends StdSerializer<Object>
{
    public final static NullSerializer instance = new NullSerializer();
    
    private NullSerializer() { super(Object.class); }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeNull();
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        return createSchemaNode("null");
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        visitor.expectNullFormat(typeHint);
    }
}

package com.fasterxml.jackson.databind.ser.std;

import tools.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

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
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider)
        throws JacksonException
    {
        gen.writeNull();
    }

    /**
     * Although this method should rarely get called, for convenience we should override
     * it, and handle it same way as "natural" types: by serializing exactly as is,
     * without type decorations. The most common possible use case is that of delegation
     * by JSON filter; caller cannot know what kind of serializer it gets handed.
     */
    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider serializers,
            TypeSerializer typeSer)
        throws JacksonException
    {
        gen.writeNull();
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitor.expectNullFormat(typeHint);
    }
}

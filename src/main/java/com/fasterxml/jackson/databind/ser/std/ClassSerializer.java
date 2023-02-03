package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;

/**
 * Also: default bean access will not do much good with Class.class. But
 * we can just serialize the class name and that should be enough.
 */
@SuppressWarnings("serial")
public class ClassSerializer
    extends StdScalarSerializer<Class<?>>
{
    public ClassSerializer() { super(Class.class, false); }

    @Override
    public void serialize(Class<?> value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        g.writeString(value.getName());
    }

    /**
     * @deprecated Since 2.15
     */
    @Deprecated
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        return createSchemaNode("string", true);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    {
        visitStringFormat(visitor, typeHint);
    }
}
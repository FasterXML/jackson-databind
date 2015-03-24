package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

/**
 * Use serializer of this class in case of type and serializer for this type can't detect statically
 * For example, if intermediate {@link com.fasterxml.jackson.databind.util.Converter} return generic Object value,
 * this serializer can be used to get actual serializer in runtime
 */
@SuppressWarnings("serial")
public class DynamicSerializer
    extends StdSerializer<Object>
{
    private final JsonSerializer<Object> chainedSerializer;

    public DynamicSerializer(JsonSerializer<Object> chainedSerializer) {
        super(Object.class);
        this.chainedSerializer = chainedSerializer;
    }
    
    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException
    {
        JsonSerializer<Object> dynamicSerializer = provider.findValueSerializer(value.getClass());
        if (dynamicSerializer != null && !provider.isDynamicSerializer(dynamicSerializer)) {
            dynamicSerializer.serialize(value, jgen, provider);
        } else {
            chainedSerializer.serialize(value, jgen, provider);
        }
    }

    @Override
    public final void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException
    {
        JsonSerializer<Object> dynamicSerializer = provider.findValueSerializer(value.getClass());
        if (dynamicSerializer != null) {
            dynamicSerializer.serializeWithType(value, jgen, provider, typeSer);
        }

        chainedSerializer.serializeWithType(value, jgen, provider, typeSer);
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException
    {
        return null;
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    { 
        visitor.expectAnyFormat(typeHint);
    }
}

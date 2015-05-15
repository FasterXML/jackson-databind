package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.type.ReferenceType;

// Since 2.6 in its own class
public class AtomicReferenceSerializer
    extends StdSerializer<AtomicReference<?>>
{
    private static final long serialVersionUID = 1L;

    /**
     * @deprecated Since 2.6
     */
    @Deprecated
    public AtomicReferenceSerializer() { super(AtomicReference.class, false); }

    public AtomicReferenceSerializer(ReferenceType type) {
        super(type);
    }
    
    @Override
    public boolean isEmpty(SerializerProvider provider, AtomicReference<?> value) {
        return (value == null) || (value.get() == null);
    }
    
    @Override
    public void serialize(AtomicReference<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        provider.defaultSerializeValue(value.get(), jgen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("any", true);
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException
    {
        visitor.expectAnyFormat(typeHint);
    }
}
package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToEmptyObjectSerializer;

@SuppressWarnings("serial")
public class UnknownSerializer
    extends ToEmptyObjectSerializer // since 2.13
{
    public UnknownSerializer() {
        super(Object.class);
    }

    // @since 2.6
    public UnknownSerializer(Class<?> cls) {
        super(cls);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider ctxt) throws IOException
    {
        // 27-Nov-2009, tatu: As per [JACKSON-201] may or may not fail...
        if (ctxt.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)) {
            failForEmpty(ctxt, value);
        }
        super.serialize(value, gen, ctxt);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider ctxt,
            TypeSerializer typeSer) throws IOException
    {
        if (ctxt.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)) {
            failForEmpty(ctxt, value);
        }
        super.serializeWithType(value, gen, ctxt, typeSer);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value) {
        return true;
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
        return null;
    }
    
    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException
    { 
        visitor.expectAnyFormat(typeHint);
    }

    protected void failForEmpty(SerializerProvider prov, Object value)
            throws JsonMappingException {
        prov.reportBadDefinition(handledType(), String.format(
                "No serializer found for class %s and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)",
                value.getClass().getName()));
    }
}

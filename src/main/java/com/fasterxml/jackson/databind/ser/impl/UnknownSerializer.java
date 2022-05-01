package com.fasterxml.jackson.databind.ser.impl;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.ToEmptyObjectSerializer;
import com.fasterxml.jackson.databind.util.NativeImageUtil;

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

    protected void failForEmpty(SerializerProvider prov, Object value)
            throws JsonMappingException {
        Class<?> cl = value.getClass();
        if (NativeImageUtil.needsReflectionConfiguration(cl)) {
            prov.reportBadDefinition(handledType(), String.format(
                    "No serializer found for class %s and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS). This appears to be a native image, in which case you may need to configure reflection for the class that is to be serialized",
                    cl.getName()));
        } else {
            prov.reportBadDefinition(handledType(), String.format(
                    "No serializer found for class %s and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)",
                    cl.getName()));
        }
    }
}

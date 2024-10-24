package com.fasterxml.jackson.databind.cfg;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNull;

class DatabindContextTest
{
    private final ObjectMapper MAPPER = new JsonMapper();

    @Test
    void deserializationContext() throws Exception
    {
        DeserializationContext ctxt = MAPPER.getDeserializationContext();
        // should be ok to try to resolve `null`
        assertNull(ctxt.constructType((Class<?>) null));
        assertNull(ctxt.constructType((java.lang.reflect.Type) null));
    }

    @Test
    void serializationContext() throws Exception
    {
        SerializerProvider ctxt = MAPPER.getSerializerProvider();
        assertNull(ctxt.constructType(null));
    }
}

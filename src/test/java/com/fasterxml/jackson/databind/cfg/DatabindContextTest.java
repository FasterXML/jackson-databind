package com.fasterxml.jackson.databind.cfg;

import com.fasterxml.jackson.databind.*;

public class DatabindContextTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    public void testDeserializationContext() throws Exception
    {
        DeserializationContext ctxt = MAPPER.getDeserializationContext();
        // should be ok to try to resolve `null`
        assertNull(ctxt.constructType((Class<?>) null));
        assertNull(ctxt.constructType((java.lang.reflect.Type) null));
    }

    public void testSerializationContext() throws Exception
    {
        SerializerProvider ctxt = MAPPER.getSerializerProvider();
        assertNull(ctxt.constructType(null));
    }
}

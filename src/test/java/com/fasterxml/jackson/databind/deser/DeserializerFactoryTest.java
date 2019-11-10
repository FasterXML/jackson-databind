package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;

public class DeserializerFactoryTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    static class POJO2539 { }

    // [databind#2539]: check existence of deserializer for type
    public void testJDKDeserializerExistence() throws Exception
    {
        DeserializationContext ctxt = MAPPER.getDeserializationContext();
        DeserializerFactory factory = ctxt.getFactory();
        DeserializationConfig config = MAPPER.getDeserializationConfig();

        // First verify some basic types
        assertTrue(factory.hasExplicitDeserializerFor(config, String.class));
        assertTrue(factory.hasExplicitDeserializerFor(config, java.math.BigDecimal.class));
        assertTrue(factory.hasExplicitDeserializerFor(config, java.net.URL.class));
        assertTrue(factory.hasExplicitDeserializerFor(config, java.util.UUID.class));

        // and also negative testing
        assertFalse(factory.hasExplicitDeserializerFor(config, Object.class));
    }
}

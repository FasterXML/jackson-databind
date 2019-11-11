package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;

public class DeserializerFactoryTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    static class POJO2539 { }

    // [databind#2539]: check existence of deserializer for type
    public void testJDKDeserializerExistence() throws Exception
    {

        // First verify some basic types
        _verifyIsFound(String.class);
        _verifyIsFound(java.math.BigDecimal.class);
        _verifyIsFound(java.net.URL.class);
        _verifyIsFound(java.util.UUID.class);

        // and also negative testing
        _verifyNotFound(Object.class);
    }

    private void _verifyIsFound(Class<?> rawType) {
        if (!_verifyDeserExistence(rawType)) {
            fail("Should have explicit deserializer for "+rawType.getName());
        }
    }

    private void _verifyNotFound(Class<?> rawType) {
        if (_verifyDeserExistence(rawType)) {
            fail("Should NOT have explicit deserializer for "+rawType.getName());
        }
    }

    private boolean _verifyDeserExistence(Class<?> rawType) {
        DeserializationContext ctxt = MAPPER.getDeserializationContext();
        DeserializerFactory factory = ctxt.getFactory();
        DeserializationConfig config = MAPPER.getDeserializationConfig();

        return factory.hasExplicitDeserializerFor(config, rawType);
    }
}

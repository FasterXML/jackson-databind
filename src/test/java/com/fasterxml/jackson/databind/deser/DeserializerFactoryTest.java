package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.*;

public class DeserializerFactoryTest extends BaseMapTest
{
    // NOTE: need custom ObjectMapper subtype to create Deserializer
    @SuppressWarnings("serial")
    static class AccessibleMapper extends ObjectMapper {
        public DefaultDeserializationContext deserializationContext() {
            return _deserializationContext();
        }
    }

    private final AccessibleMapper MAPPER = new AccessibleMapper();

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
        DeserializationContext ctxt = MAPPER.deserializationContext();
        DeserializerFactory factory = ctxt.getFactory();

        return factory.hasExplicitDeserializerFor(ctxt, rawType);
    }
}

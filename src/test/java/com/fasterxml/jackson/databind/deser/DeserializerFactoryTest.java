package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.databind.*;

// Tests for [databind#2539] for checking whether given (raw) type has explicit
// deserializer associated
public class DeserializerFactoryTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    static class POJO2539 { }

    // [databind#2539]: check existence of deserializer for type
    public void testJDKScalarDeserializerExistence() throws Exception
    {
        // First verify some basic types
        _verifyIsFound(String.class);
        _verifyIsFound(Float.class);
        _verifyIsFound(java.math.BigDecimal.class);
        _verifyIsFound(java.net.URL.class);
        _verifyIsFound(UUID.class);

        _verifyIsFound(Calendar.class);
        _verifyIsFound(GregorianCalendar.class);
        _verifyIsFound(Date.class);

        // "Untyped" as target is actually undefined: we could choose either way.
        // It is valid target, which would support inclusion... but it is not actual
        // final target (but convenient marker) and as such never to be included as
        // type id. But more importantly there is possiblity of misuse to validate
        // base types: {@link Object} should rarely if ever be allowed as that.
//        _verifyIsFound(java.util.Object.class);
    }

    public void testJDKContainerDeserializerExistence() throws Exception
    {
        // Both general and specific container types should be considered supported
        _verifyIsFound(Collection.class);
        _verifyIsFound(List.class);
        _verifyIsFound(Map.class);
        _verifyIsFound(Set.class);

        _verifyIsFound(ArrayList.class);
        _verifyIsFound(HashMap.class);
        _verifyIsFound(LinkedHashMap.class);
        _verifyIsFound(HashSet.class);
    }

    public void testJDKArraysOfExistence() throws Exception
    {
        // Similarly, array types of all supported types should be allowed
        _verifyIsFound(String[].class);
        _verifyIsFound(java.math.BigDecimal[].class);
        _verifyIsFound(java.net.URL[].class);
        _verifyIsFound(UUID[].class);
    }

    public void testNoDeserTypes() throws Exception
    {
        // Types for which we should NOT have explicit deserializer


        // In general, arbitrary POJOs do not...
        _verifyNotFound(POJO2539.class);

        // but also just in case someone found a way to abuse JDK types,
        // we would not want to allow that
        _verifyNotFound(java.lang.Process.class);
        _verifyNotFound(java.lang.System.class);
        _verifyNotFound(java.lang.Thread.class);
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

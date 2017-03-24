package com.fasterxml.jackson.databind.struct;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/**
 * Tests to verify implementation of [databind#540]; also for
 * follow up work of:
 *
 * - [databind#994]
 */
public class EmptyArrayAsNullTest extends BaseMapTest
{
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final ObjectReader DEFAULT_READER = MAPPER.reader();
    private final ObjectReader READER_WITH_ARRAYS = DEFAULT_READER
            .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

    static class Bean {
        public String a = "foo";
    }

    final static String EMPTY_ARRAY = "  [\n]";

    /*
    /**********************************************************
    /* Test methods, settings
    /**********************************************************
     */

    public void testSettings() {
        assertFalse(MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
        assertFalse(DEFAULT_READER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
        assertTrue(READER_WITH_ARRAYS.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
    }

    /*
    /**********************************************************
    /* Test methods, POJOs
    /**********************************************************
     */

    // [databind#540]
    public void testPOJOFromEmptyArray() throws Exception
    {
        // first, verify default settings which do not accept empty Array
        try {
            DEFAULT_READER.forType(Bean.class)
                .readValue(EMPTY_ARRAY);
            fail("Should not accept Empty Array for POJO by default");
        } catch (JsonMappingException e) {
            verifyException(e, "START_ARRAY token");
            assertValidLocation(e.getLocation());
        }

        // should be ok to enable dynamically:
        Bean result = READER_WITH_ARRAYS.forType(Bean.class)
                .readValue(EMPTY_ARRAY);
        assertNull(result);
    }

    /*
    /**********************************************************
    /* Test methods, Maps
    /**********************************************************
     */

    public void testMapFromEmptyArray() throws Exception
    {
        // first, verify default settings which do not accept empty Array
        try {
            DEFAULT_READER.forType(Map.class)
                .readValue(EMPTY_ARRAY);
            fail("Should not accept Empty Array for Map by default");
        } catch (JsonMappingException e) {
            verifyException(e, "START_ARRAY token");
        }
        // should be ok to enable dynamically:
        Map<?,?> result = READER_WITH_ARRAYS.forType(Map.class)
                .readValue(EMPTY_ARRAY);
        assertNull(result);
    }

    public void testEnumMapFromEmptyArray() throws Exception
    {
    
        EnumMap<?,?> result2 = READER_WITH_ARRAYS.forType(new TypeReference<EnumMap<ABC,String>>() { })
                .readValue(EMPTY_ARRAY);
        assertNull(result2);
    }

    /*
    /**********************************************************
    /* Test methods, primitives/wrappers
    /**********************************************************
     */

    public void testWrapperFromEmptyArray() throws Exception
    {
//        _testNullWrapper(Boolean.class);
//        _testNullWrapper(Byte.class);
        _testNullWrapper(Character.class);
//        _testNullWrapper(Short.class);
//        _testNullWrapper(Integer.class);
//        _testNullWrapper(Long.class);
//        _testNullWrapper(Float.class);
//        _testNullWrapper(Double.class);
    }

    /*
    /**********************************************************
    /* Test methods, other
    /**********************************************************
     */

    public void testNullStringFromEmptyArray() throws Exception {
        _testNullWrapper(String.class);
    }

    public void testNullEnumFromEmptyArray() throws Exception {
        _testNullWrapper(ABC.class);
    }

    public void testStdJdkTypesFromEmptyArray() throws Exception
    {
        _testNullWrapper(BigInteger.class);
        _testNullWrapper(BigDecimal.class);

        _testNullWrapper(UUID.class);

        /*
        _testNullWrapper(Date.class);
        _testNullWrapper(Calendar.class);
        */
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void _testNullWrapper(Class<?> cls) throws Exception
    {
        Object result = READER_WITH_ARRAYS.forType(cls).readValue(EMPTY_ARRAY);
        assertNull(result);
    }
}

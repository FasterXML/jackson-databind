package com.fasterxml.jackson.databind.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

/**
 * Tests to verify implementation of [databind#540]; also for
 * follow up work of:
 *
 * - [databind#994]
 */
public class CoerceEmptyArrayTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();
    private final ObjectReader DEFAULT_READER = DEFAULT_MAPPER.reader();
    private final ObjectReader READER_WITH_ARRAYS = DEFAULT_READER
            .with(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsEmpty))
            .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.TryConvert))
            .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
            cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsNull))
        .build();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
            cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.Fail))
        .build();

    static class Bean {
        public String a = "foo";

        @Override
        public boolean equals(Object o) {
            return (o instanceof Bean)
                    && a.equals(((Bean) o).a);
        }
    }

    final static String EMPTY_ARRAY = "  [\n]";

    /*
    /**********************************************************
    /* Test methods, settings
    /**********************************************************
     */

    public void testSettings() {
        assertFalse(DEFAULT_MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT));
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
        final Class<?> targetType = Bean.class;

        _verifyFailForEmptyArray(DEFAULT_READER, targetType);
        _verifyFailForEmptyArray(MAPPER_TO_FAIL, targetType);

        // Nulls for explicit, "TryConvert"
        _verifyToNullCoercion(MAPPER_TO_NULL, targetType);
        _verifyToNullCoercion(MAPPER_TRY_CONVERT, targetType);

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, targetType, new Bean());

        // But let's also check precedence: legacy setting allow, but mask for type
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .withCoercionConfig(targetType, cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.Fail))
                .build();
        _verifyFailForEmptyArray(mapper, targetType);

        // and conversely
        mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .withCoercionConfig(LogicalType.POJO, cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyArray, CoercionAction.AsEmpty))
                .build();
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, targetType, new Bean());
    }

    /*
    /**********************************************************
    /* Test methods, Maps
    /**********************************************************
     */

    public void testMapFromEmptyArray() throws Exception
    {
        final Class<?> targetType = Map.class;

        _verifyFailForEmptyArray(DEFAULT_READER, targetType);
        _verifyFailForEmptyArray(MAPPER_TO_FAIL, targetType);

        // Nulls for explicit, "TryConvert"
        _verifyToNullCoercion(MAPPER_TO_NULL, targetType);
        _verifyToNullCoercion(MAPPER_TRY_CONVERT, targetType);

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, targetType, new LinkedHashMap<>());

        // assume overrides work ok since POJOs test it
    }

    public void testEnumMapFromEmptyArray() throws Exception
    {
        final JavaType targetType = DEFAULT_READER.getTypeFactory()
                .constructType(new TypeReference<EnumMap<ABC,String>>() { });

        assertNull(MAPPER_TO_NULL.readerFor(targetType).readValue(EMPTY_ARRAY));

        EnumMap<?,?> result = MAPPER_TO_EMPTY.readerFor(targetType).readValue(EMPTY_ARRAY);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /*
    /**********************************************************
    /* Test methods, scalars
    /**********************************************************
     */

    public void testNumbersFromEmptyArray() throws Exception
    {
        for (Class<?> targetType : new Class<?>[] {
            Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class,
            BigInteger.class, BigDecimal.class
        }) {
            // Default, fail; explicit fail
            _verifyFailForEmptyArray(DEFAULT_READER, targetType);
            _verifyFailForEmptyArray(MAPPER_TO_FAIL, targetType);

            // Nulls for explicit, "TryConvert"
            _verifyToNullCoercion(MAPPER_TO_NULL, targetType);
            _verifyToNullCoercion(MAPPER_TRY_CONVERT, targetType);
        }

        // But as-empty needs separate
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Boolean.class, Boolean.FALSE);
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Character.class, Character.valueOf('\0'));

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Byte.class, Byte.valueOf((byte) 0));
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Short.class, Short.valueOf((short) 0));
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Integer.class, Integer.valueOf(0));
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Long.class, Long.valueOf(0L));

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Float.class, Float.valueOf(0f));
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, Double.class, Double.valueOf(0d));

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, BigInteger.class, BigInteger.ZERO);
        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, BigDecimal.class, new BigDecimal(BigInteger.ZERO));
    }

    public void testOtherScalarsFromEmptyArray() throws Exception
    {
        for (Class<?> targetType : new Class<?>[] {
            String.class, StringBuilder.class,
            UUID.class, URL.class, URI.class,
            Date.class, Calendar.class
        }) {
            _verifyFailForEmptyArray(DEFAULT_READER, targetType);
            _verifyFailForEmptyArray(MAPPER_TO_FAIL, targetType);

            // Nulls for explicit, "TryConvert"
            _verifyToNullCoercion(MAPPER_TO_NULL, targetType);
            _verifyToNullCoercion(MAPPER_TRY_CONVERT, targetType);
        }

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, String.class, "");
        StringBuilder sb = MAPPER_TO_EMPTY.readerFor(StringBuilder.class)
                .readValue(EMPTY_ARRAY);
        assertEquals(0, sb.length());

        _verifyToEmptyCoercion(MAPPER_TO_EMPTY, UUID.class, new UUID(0L, 0L));
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _verifyToNullCoercion(ObjectMapper mapper, Class<?> cls) throws Exception {
        _verifyToNullCoercion(mapper.reader(), cls);
    }

    private void _verifyToNullCoercion(ObjectReader r, Class<?> cls) throws Exception {
        Object result = r.forType(cls).readValue(EMPTY_ARRAY);
        if (result != null) {
            fail("Expect null for "+cls.getName()+", got: "+result);
        }
    }

    private void _verifyToEmptyCoercion(ObjectMapper mapper, Class<?> cls, Object exp) throws Exception {
        _verifyToEmptyCoercion(mapper.reader(), cls, exp);
    }

    private void _verifyToEmptyCoercion(ObjectReader r, Class<?> cls, Object exp) throws Exception {
        Object result = r.forType(cls).readValue(EMPTY_ARRAY);
        if (!exp.equals(result)) {
            fail("Expect value ["+exp+"] for "+cls.getName()+", got: "+result);
        }
    }

    private void _verifyFailForEmptyArray(ObjectMapper mapper, Class<?> targetType) throws Exception {
        _verifyFailForEmptyArray(mapper.readerFor(targetType), targetType);
    }

    private void _verifyFailForEmptyArray(ObjectReader r, Class<?> targetType) throws Exception
    {
        try {
            r.forType(targetType).readValue(EMPTY_ARRAY);
            fail("Should not accept Empty Array for "+targetType.getName()+" by default");
        } catch (MismatchedInputException e) {
            verifyException(e, "from Array value (token `JsonToken.START_ARRAY`)");
        }
    }
}

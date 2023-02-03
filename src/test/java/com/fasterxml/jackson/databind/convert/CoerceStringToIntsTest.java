package com.fasterxml.jackson.databind.convert;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.LogicalType;

public class CoerceStringToIntsTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();
    private final ObjectReader READER_LEGACY_FAIL = jsonMapperBuilder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .build()
            .reader();

    private final ObjectMapper MAPPER_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Integer, cfg ->
            cfg.setCoercion(CoercionInputShape.String, CoercionAction.AsEmpty))
        .build();

    private final ObjectMapper MAPPER_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Integer, cfg ->
            cfg.setCoercion(CoercionInputShape.String, CoercionAction.TryConvert))
        .build();

    private final ObjectMapper MAPPER_TO_NULL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Integer, cfg ->
            cfg.setCoercion(CoercionInputShape.String, CoercionAction.AsNull))
        .build();

    private final ObjectMapper MAPPER_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfig(LogicalType.Integer, cfg ->
            cfg.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
        .build();

    /*
    /********************************************************
    /* Test methods, defaults (legacy)
    /********************************************************
     */

    public void testLegacyStringToIntCoercion() throws Exception
    {
        // by default, should be ok
        Integer I = DEFAULT_MAPPER.readValue(q("28"), Integer.class);
        assertEquals(28, I.intValue());
        {
            IntWrapper w = DEFAULT_MAPPER.readValue(a2q("{'i':'37' }"), IntWrapper.class);
            assertEquals(37, w.i);
            int[] arr = DEFAULT_MAPPER.readValue(a2q("[ '42' ]"), int[].class);
            assertEquals(42, arr[0]);
        }

        Long L = DEFAULT_MAPPER.readValue(q("39"), Long.class);
        assertEquals(39L, L.longValue());
        {
            LongWrapper w = DEFAULT_MAPPER.readValue(a2q("{'l':'-13' }"), LongWrapper.class);
            assertEquals(-13L, w.l);
            long[] arr = DEFAULT_MAPPER.readValue(a2q("[ '0' ]"), long[].class);
            assertEquals(0L, arr[0]);
        }

        Short S = DEFAULT_MAPPER.readValue(q("42"), Short.class);
        assertEquals(42, S.intValue());

        BigInteger biggie = DEFAULT_MAPPER.readValue(q("95007"), BigInteger.class);
        assertEquals(95007, biggie.intValue());
    }

    public void testLegacyFailStringToInt() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.class, q("52"), "java.lang.Integer");
        _verifyCoerceFail(READER_LEGACY_FAIL, Integer.TYPE, q("37"), "int");
        _verifyCoerceFail(READER_LEGACY_FAIL, IntWrapper.class, "{\"i\":\"19\" }", "int");
        _verifyCoerceFail(READER_LEGACY_FAIL, int[].class, "[ \"-128\" ]", "element of `int[]`");
    }

    public void testLegacyFailStringToLong() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Long.class, q("55"));
        _verifyCoerceFail(READER_LEGACY_FAIL, Long.TYPE, q("-25"));
        _verifyCoerceFail(READER_LEGACY_FAIL, LongWrapper.class, "{\"l\": \"77\" }");
        _verifyCoerceFail(READER_LEGACY_FAIL, long[].class, "[ \"136\" ]", "element of `long[]`");
    }

    public void testLegacyFailStringToOther() throws Exception
    {
        _verifyCoerceFail(READER_LEGACY_FAIL, Short.class, q("50"));
        _verifyCoerceFail(READER_LEGACY_FAIL, Short.TYPE, q("-255"));
        _verifyCoerceFail(READER_LEGACY_FAIL, short[].class, "[ \"-126\" ]", "element of `short[]`");

        _verifyCoerceFail(READER_LEGACY_FAIL, Byte.class, q("60"));
        _verifyCoerceFail(READER_LEGACY_FAIL, Byte.TYPE, q("-25"));
        _verifyCoerceFail(READER_LEGACY_FAIL, byte[].class, "[ \"-13\" ]", "element of `byte[]`");

        _verifyCoerceFail(READER_LEGACY_FAIL, BigInteger.class, q("25236"));

        _verifyCoerceFail(READER_LEGACY_FAIL, AtomicLong.class, q("25236"));
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, to null
    /********************************************************
     */

    public void testCoerceConfigStringToNull() throws Exception
    {
        assertNull(MAPPER_TO_NULL.readValue(q("155"), Integer.class));
        // `null` not possible for primitives, must use empty (aka default) value
        assertEquals(Integer.valueOf(0), MAPPER_TO_NULL.readValue(q("-178"), Integer.TYPE));
        {
            IntWrapper w = MAPPER_TO_NULL.readValue( "{\"i\":\"-225\" }", IntWrapper.class);
            assertEquals(0, w.i);
            int[] ints = MAPPER_TO_NULL.readValue("[ \"26\" ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(0, ints[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue(q("25"), Long.class));
        assertEquals(Long.valueOf(0L), MAPPER_TO_NULL.readValue(q("-425"), Long.TYPE));
        {
            LongWrapper w = MAPPER_TO_NULL.readValue( "{\"l\":\"-225\" }", LongWrapper.class);
            assertEquals(0L, w.l);
            long[] l = MAPPER_TO_NULL.readValue("[ \"190\" ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(0L, l[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue(q("25"), Short.class));
        assertEquals(Short.valueOf((short) 0), MAPPER_TO_NULL.readValue(q("-425"), Short.TYPE));
        {
            short[] s = MAPPER_TO_NULL.readValue("[ \"25\" ]", short[].class);
            assertEquals(1, s.length);
            assertEquals((short) 0, s[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue(q("29"), Byte.class));
        assertEquals(Byte.valueOf((byte) 0), MAPPER_TO_NULL.readValue(q("-425"), Byte.TYPE));
        {
            byte[] arr = MAPPER_TO_NULL.readValue("[ \"25\" ]", byte[].class);
            assertEquals(1, arr.length);
            assertEquals((byte) 0, arr[0]);
        }

        assertNull(MAPPER_TO_NULL.readValue(q("25000000"), BigInteger.class));
        {
            BigInteger[] arr = MAPPER_TO_NULL.readValue("[ \"25\" ]", BigInteger[].class);
            assertEquals(1, arr.length);
            assertNull(arr[0]);
        }
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, to empty
    /********************************************************
     */

    public void testCoerceConfigStringToEmpty() throws Exception
    {
        assertEquals(Integer.valueOf(0), MAPPER_TO_EMPTY.readValue(q("12"), Integer.class));
        assertEquals(Integer.valueOf(0), MAPPER_TO_EMPTY.readValue(q("15"), Integer.TYPE));
        {
            IntWrapper w = MAPPER_TO_EMPTY.readValue( "{\"i\":\"-225\" }", IntWrapper.class);
            assertEquals(0, w.i);
            int[] ints = MAPPER_TO_EMPTY.readValue("[ \"25\" ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(0, ints[0]);
        }

        assertEquals(Long.valueOf(0), MAPPER_TO_EMPTY.readValue(q("12"), Long.class));
        assertEquals(Long.valueOf(0), MAPPER_TO_EMPTY.readValue(q("99"), Long.TYPE));
        {
            LongWrapper w = MAPPER_TO_EMPTY.readValue( "{\"l\":\"-225\" }", LongWrapper.class);
            assertEquals(0L, w.l);
            long[] l = MAPPER_TO_EMPTY.readValue("[ \"26\" ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(0L, l[0]);
        }

        assertEquals(Short.valueOf((short)0), MAPPER_TO_EMPTY.readValue(q("12"), Short.class));
        assertEquals(Short.valueOf((short) 0), MAPPER_TO_EMPTY.readValue(q("999"), Short.TYPE));

        assertEquals(Byte.valueOf((byte)0), MAPPER_TO_EMPTY.readValue(q("12"), Byte.class));
        assertEquals(Byte.valueOf((byte) 0), MAPPER_TO_EMPTY.readValue(q("123"), Byte.TYPE));

        assertEquals(BigInteger.valueOf(0L), MAPPER_TO_EMPTY.readValue(q("1234"), BigInteger.class));
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, coerce
    /********************************************************
     */

    public void testCoerceConfigStringConvert() throws Exception
    {
        assertEquals(Integer.valueOf(12), MAPPER_TRY_CONVERT.readValue(q("12"), Integer.class));
        assertEquals(Integer.valueOf(34), MAPPER_TRY_CONVERT.readValue(q("34"), Integer.TYPE));
        {
            IntWrapper w = MAPPER_TRY_CONVERT.readValue( "{\"i\":\"-225\" }", IntWrapper.class);
            assertEquals(-225, w.i);
            int[] ints = MAPPER_TRY_CONVERT.readValue("[ \"2210\" ]", int[].class);
            assertEquals(1, ints.length);
            assertEquals(2210, ints[0]);
        }

        assertEquals(Long.valueOf(34), MAPPER_TRY_CONVERT.readValue(q("34"), Long.class));
        assertEquals(Long.valueOf(534), MAPPER_TRY_CONVERT.readValue(q("534"), Long.TYPE));
        {
            LongWrapper w = MAPPER_TRY_CONVERT.readValue( "{\"l\":\"-225\" }", LongWrapper.class);
            assertEquals(-225L, w.l);
            long[] l = MAPPER_TRY_CONVERT.readValue("[ \"22\" ]", long[].class);
            assertEquals(1, l.length);
            assertEquals(22L, l[0]);
        }

        assertEquals(Short.valueOf((short)12), MAPPER_TRY_CONVERT.readValue(q("12"), Short.class));
        assertEquals(Short.valueOf((short) 344), MAPPER_TRY_CONVERT.readValue(q("344"), Short.TYPE));

        assertEquals(Byte.valueOf((byte)12), MAPPER_TRY_CONVERT.readValue(q("12"), Byte.class));
        assertEquals(Byte.valueOf((byte) -99), MAPPER_TRY_CONVERT.readValue(q("-99"), Byte.TYPE));

        assertEquals(BigInteger.valueOf(1242L), MAPPER_TRY_CONVERT.readValue(q("1242"), BigInteger.class));
    }

    /*
    /********************************************************
    /* Test methods, CoerceConfig, fail
    /********************************************************
     */

    public void testCoerceConfigFailFromString() throws Exception
    {
        _verifyCoerceFail(MAPPER_TO_FAIL, Integer.class, q("15"));
        _verifyCoerceFail(MAPPER_TO_FAIL, Integer.TYPE, q("15"));
        _verifyCoerceFail(MAPPER_TO_FAIL, IntWrapper.class, "{\"i\":\"-225\" }", "int");
        _verifyCoerceFail(MAPPER_TO_FAIL, int[].class, "[ \"256\" ]", "element of `int[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Long.class, q("738"));
        _verifyCoerceFail(MAPPER_TO_FAIL, Long.TYPE, q("-99"));
        _verifyCoerceFail(MAPPER_TO_FAIL, LongWrapper.class, "{\"l\": \"77\" }");
        _verifyCoerceFail(MAPPER_TO_FAIL, long[].class, "[ \"-135\" ]", "element of `long[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Short.class, q("-19"));
        _verifyCoerceFail(MAPPER_TO_FAIL, Short.TYPE, q("25"));
        _verifyCoerceFail(MAPPER_TO_FAIL, short[].class, "[ \"-135\" ]", "element of `short[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, Byte.class, q("15"));
        _verifyCoerceFail(MAPPER_TO_FAIL, Byte.TYPE, q("-25"));
        _verifyCoerceFail(MAPPER_TO_FAIL, byte[].class, "[ \"-1379\" ]", "element of `byte[]`");

        _verifyCoerceFail(MAPPER_TO_FAIL, BigInteger.class, q("25236256"));
    }

    /*
    /********************************************************
    /* Helper methods
    /********************************************************
     */

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
            String doc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectMapper m, Class<?> targetType,
            String doc, String targetTypeDesc) throws Exception
    {
        _verifyCoerceFail(m.reader(), targetType, doc, targetTypeDesc);
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
            String doc) throws Exception
    {
        _verifyCoerceFail(r, targetType, doc, targetType.getName());
    }

    private void _verifyCoerceFail(ObjectReader r, Class<?> targetType,
            String doc, String targetTypeDesc) throws Exception
    {
        try {
            r.forType(targetType).readValue(doc);
            fail("Should not accept String for "+targetType.getName()+" by default");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce String");
            verifyException(e, targetTypeDesc);
        }
    }
}

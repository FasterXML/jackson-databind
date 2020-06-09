package com.fasterxml.jackson.databind.convert;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class CoerceMiscScalarsTest extends BaseMapTest
{
    private final ObjectMapper MAPPER_EMPTY_TO_FAIL = jsonMapperBuilder()
                .withCoercionConfigDefaults(cfg ->
                    cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
                .build();

    private final ObjectMapper MAPPER_EMPTY_TO_EMPTY = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsEmpty))
            .build();

    private final ObjectMapper MAPPER_EMPTY_TO_TRY_CONVERT = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.TryConvert))
            .build();
    
    private final ObjectMapper MAPPER_EMPTY_TO_NULL = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
            .build();

    private final String JSON_EMPTY = quote("");

    /*
    /********************************************************
    /* Test methods, successful coercions from empty String
    /********************************************************
     */
    
    public void testScalarEmptyToNull() throws Exception
    {
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, UUID.class);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, File.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, URL.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, URI.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Class.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, JavaType.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Currency.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Pattern.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Locale.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Charset.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, TimeZone.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, InetAddress.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, InetSocketAddress.class);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, StringBuilder.class);
    }

    public void testScalarEmptyToEmpty() throws Exception
    {
        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, UUID.class,
                new UUID(0L, 0L));

        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, URI.class,
                URI.create(""));
        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, Locale.class,
                Locale.ROOT);

        {
            StringBuilder result = MAPPER_EMPTY_TO_EMPTY.readValue(JSON_EMPTY, StringBuilder.class);
            assertNotNull(result);
            assertEquals(0, result.length());
        }
    }

    public void testScalarEmptyToTryConvert() throws Exception
    {
        // Should be same as `AsNull` for all
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, UUID.class);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, File.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, URL.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, URI.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Class.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, JavaType.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Currency.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Pattern.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Locale.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Charset.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, TimeZone.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, InetAddress.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, InetSocketAddress.class);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, StringBuilder.class);
    }

    /*
    /********************************************************
    /* Test methods, failed coercions from empty String
    /********************************************************
     */

    public void testScalarsFailFromEmpty() throws Exception
    {
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, UUID.class);
    }

    /*
    /********************************************************
    /* Second-level test helper methods
    /********************************************************
     */
    
    private void _testScalarEmptyToNull(ObjectMapper mapper, Class<?> target) throws Exception
    {
        assertNull(mapper.readerFor(target).readValue(JSON_EMPTY));
    }

    private void _testScalarEmptyToEmpty(ObjectMapper mapper,
            Class<?> target, Object emptyValue) throws Exception
    {
        Object result = mapper.readerFor(target).readValue(JSON_EMPTY);
        assertEquals(emptyValue, result);
    }

    private void _verifyScalarToFail(ObjectMapper mapper, Class<?> target) throws Exception
    {
        try {
            /*Object result =*/ mapper.readerFor(target)
                .readValue(JSON_EMPTY);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String ");
            verifyException(e, " to `"+target.getName());
        }
    }
}

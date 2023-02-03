package com.fasterxml.jackson.databind.convert;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
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
    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();

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

    private final ObjectMapper MAPPER_EMPTY_TO_FAIL = jsonMapperBuilder()
            .withCoercionConfigDefaults(cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
            .build();

    private final String JSON_EMPTY = q("");

    /*
    /**********************************************************************
    /* Test methods, defaults (legacy)
    /**********************************************************************
     */

    public void testScalarDefaultsFromEmpty() throws Exception
    {
        // mostly as null, with some exceptions

        _testScalarEmptyToNull(DEFAULT_MAPPER, File.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, URL.class);

        _testScalarEmptyToEmpty(DEFAULT_MAPPER, URI.class,
                URI.create(""));

        _testScalarEmptyToNull(DEFAULT_MAPPER, Class.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, JavaType.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, Currency.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, Pattern.class);

        _testScalarEmptyToEmpty(DEFAULT_MAPPER, Locale.class,
                Locale.ROOT);

        _testScalarEmptyToNull(DEFAULT_MAPPER, Charset.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, TimeZone.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, InetAddress.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, InetSocketAddress.class);
    }

    /*
    /**********************************************************************
    /* Test methods, successful coercions from empty String
    /**********************************************************************
     */

    public void testScalarEmptyToNull() throws Exception
    {
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
    }

    public void testScalarEmptyToEmpty() throws Exception
    {
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, File.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, URL.class);

        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, URI.class,
                URI.create(""));

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, Class.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, JavaType.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, Currency.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, Pattern.class);

        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, Locale.class,
                Locale.ROOT);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, Charset.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, TimeZone.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, InetAddress.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_EMPTY, InetSocketAddress.class);
    }

    public void testScalarEmptyToTryConvert() throws Exception
    {
        // Should be same as `AsNull` for most but not all
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, File.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, URL.class);

        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_TRY_CONVERT, URI.class,
                URI.create(""));

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Class.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, JavaType.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Currency.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Pattern.class);

        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_TRY_CONVERT, Locale.class,
                Locale.ROOT);

        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Charset.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, TimeZone.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, InetAddress.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, InetSocketAddress.class);
    }

    /*
    /**********************************************************************
    /* Test methods, failed coercions from empty String
    /**********************************************************************
     */

    public void testScalarsFailFromEmpty() throws Exception
    {
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, File.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, URL.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, URI.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Class.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, JavaType.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Currency.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Pattern.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Locale.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Charset.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, TimeZone.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, InetAddress.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, InetSocketAddress.class);
    }

    /*
    /********************************************************
    /* Test methods, (more) special type(s)
    /********************************************************
     */

    // UUID is quite compatible, but not exactly due to historical reasons;
    // also uses custom subtype, so test separately

    public void testUUIDCoercions() throws Exception
    {
        // Coerce to `null` both by default, "TryConvert" and explicit
        _testScalarEmptyToNull(DEFAULT_MAPPER, UUID.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, UUID.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, UUID.class);

        // but allow separate "empty" value is specifically requested
        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, UUID.class,
                new UUID(0L, 0L));

        // allow forcing failure, too
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, UUID.class);

        // and allow failure with specifically configured per-class override, too
        ObjectMapper failMapper = jsonMapperBuilder()
                .withCoercionConfig(UUID.class, cfg ->
                cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail))
            .build();
        _verifyScalarToFail(failMapper, UUID.class);
    }

    // StringBuilder is its own special type, since it naturally maps
    // from String values, hence separate testing
    public void testStringBuilderCoercions() throws Exception
    {
        // should result in an "empty" StringBuilder for all valid settings
        _checkEmptyStringBuilder(DEFAULT_MAPPER.readValue(JSON_EMPTY, StringBuilder.class));
        _checkEmptyStringBuilder(MAPPER_EMPTY_TO_EMPTY.readValue(JSON_EMPTY, StringBuilder.class));
        _checkEmptyStringBuilder(MAPPER_EMPTY_TO_TRY_CONVERT.readValue(JSON_EMPTY, StringBuilder.class));
        _checkEmptyStringBuilder(MAPPER_EMPTY_TO_NULL.readValue(JSON_EMPTY, StringBuilder.class));
        // and even alleged failure should not result in that since it's not coercion
        _checkEmptyStringBuilder(MAPPER_EMPTY_TO_FAIL.readValue(JSON_EMPTY, StringBuilder.class));
    }

    private void _checkEmptyStringBuilder(StringBuilder sb) {
        assertNotNull(sb);
        assertEquals(0, sb.length());
    }

    // Date, Calendar also included here for convenience

    public void testLegacyDateTimeCoercions() throws Exception
    {
        // Coerce to `null` both by default, "TryConvert" and explicit
        _testScalarEmptyToNull(DEFAULT_MAPPER, Calendar.class);
        _testScalarEmptyToNull(DEFAULT_MAPPER, Date.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Calendar.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_NULL, Date.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Calendar.class);
        _testScalarEmptyToNull(MAPPER_EMPTY_TO_TRY_CONVERT, Date.class);

        // but allow separate "empty" value is specifically requested
        Calendar emptyCal = new GregorianCalendar();
        emptyCal.setTimeInMillis(0L);
//        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, Calendar.class, emptyCal);
        _testScalarEmptyToEmpty(MAPPER_EMPTY_TO_EMPTY, Date.class, new Date(0L));

        // allow forcing failure, too
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Calendar.class);
        _verifyScalarToFail(MAPPER_EMPTY_TO_FAIL, Date.class);
    }

    /*
    /**********************************************************************
    /* Second-level test helper methods
    /**********************************************************************
     */

    private void _testScalarEmptyToNull(ObjectMapper mapper, Class<?> target) throws Exception
    {
        assertNull(mapper.readerFor(target).readValue(JSON_EMPTY));
    }

    private void _testScalarEmptyToEmpty(ObjectMapper mapper,
            Class<?> target, Object emptyValue) throws Exception
    {
        Object result = mapper.readerFor(target).readValue(JSON_EMPTY);
        if (result == null) {
            fail("Expected empty, non-null value for "+target.getName()+", got null");
        }
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

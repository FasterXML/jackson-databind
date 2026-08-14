package com.fasterxml.jackson.databind.deser.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class BigNumbersDeserTest
    extends DatabindTestUtil
{
    static class BigIntegerWrapper {
        public BigInteger number;
    }

    static class BigDecimalWrapper {
        public BigDecimal number;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectMapper newJsonMapperWithUnlimitedNumberSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    @Test
    public void testDouble() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("d"), DoubleWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
    public void testDoubleUnlimited() throws Exception
    {
        DoubleWrapper dw =
            newJsonMapperWithUnlimitedNumberSizeSupport().readValue(generateJson("d"), DoubleWrapper.class);
        assertNotNull(dw);
    }

    @Test
    public void testBigDecimal() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigDecimalWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ", "exceeds the maximum allowed");
        }
    }

    @Test
    public void testBigDecimalUnlimited() throws Exception
    {
        BigDecimalWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigDecimalWrapper.class);
        assertNotNull(bdw);
    }

    @Test
    public void testBigInteger() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigIntegerWrapper.class);
            fail("expected StreamReadException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length", "exceeds the maximum allowed");
        }
    }

    @Test
    public void testBigIntegerUnlimited() throws Exception
    {
        BigIntegerWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigIntegerWrapper.class);
        assertNotNull(bdw);
    }

    // [databind#6165]
    // Number length limits must apply to `BigInteger`/`BigDecimal` used as Map keys,
    // not just as values: the key path fed strings straight to the O(n^2)
    // `BigInteger(String)`/`BigDecimal(String)` constructors, bounded only by the far
    // larger max-name-length limit.
    @Test
    public void testBigIntegerAsMapKey() throws Exception
    {
        _verifyKeyTooLong(MAPPER, _digits(1200), new TypeReference<Map<BigInteger, String>>() { });
    }

    @Test
    public void testBigDecimalAsMapKey() throws Exception
    {
        _verifyKeyTooLong(MAPPER, _digits(1200), new TypeReference<Map<BigDecimal, String>>() { });
    }

    // Same limit applies to `Float`/`Double` keys as well
    @Test
    public void testFloatingPointAsMapKey() throws Exception
    {
        _verifyKeyTooLong(MAPPER, _digits(1200), new TypeReference<Map<Double, String>>() { });
        _verifyKeyTooLong(MAPPER, _digits(1200), new TypeReference<Map<Float, String>>() { });
    }

    @Test
    public void testBigNumberMapKeysWithinLimit() throws Exception
    {
        Map<BigInteger, String> mi = MAPPER.readValue("{\"12345678901234567890\":\"a\"}",
                new TypeReference<Map<BigInteger, String>>() { });
        assertEquals("a", mi.get(new BigInteger("12345678901234567890")));

        Map<BigDecimal, String> md = MAPPER.readValue("{\"3.14159265358979\":\"b\"}",
                new TypeReference<Map<BigDecimal, String>>() { });
        assertEquals("b", md.get(new BigDecimal("3.14159265358979")));
    }

    // Limit is inclusive: key of exactly `maxNumberLength` digits still accepted,
    // one digit more is not
    @Test
    public void testMapKeyAtLengthLimit() throws Exception
    {
        final int maxLen = StreamReadConstraints.defaults().getMaxNumberLength();
        final String key = _digits(maxLen);

        Map<BigInteger, String> m = MAPPER.readValue("{\"" + key + "\":\"a\"}",
                new TypeReference<Map<BigInteger, String>>() { });
        assertEquals("a", m.get(new BigInteger(key)));

        _verifyKeyTooLong(MAPPER, _digits(maxLen + 1),
                new TypeReference<Map<BigInteger, String>>() { });
    }

    // Limit is configurable for keys just like it is for values
    @Test
    public void testMapKeyWithLoweredLengthLimit() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(20).build())
                .build();
        // 25 digits: acceptable with defaults, too long here
        _verifyKeyTooLong(JsonMapper.builder(f).build(), _digits(25),
                new TypeReference<Map<BigInteger, String>>() { });
    }

    // Malformed (but short enough) keys must still fail as regular "weird key" problems
    @Test
    public void testMalformedBigNumberMapKey() throws Exception
    {
        try {
            MAPPER.readValue("{\"abc\":\"a\"}", new TypeReference<Map<BigInteger, String>>() { });
            fail("expected InvalidFormatException");
        } catch (InvalidFormatException e) {
            verifyException(e, "Cannot deserialize Map key of type `java.math.BigInteger`");
        }
    }

    private void _verifyKeyTooLong(ObjectMapper mapper, String key, TypeReference<?> targetType)
        throws Exception
    {
        try {
            mapper.readValue("{\"" + key + "\":\"x\"}", targetType);
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length ("+key.length()+") exceeds the maximum allowed");
        }
    }

    private String _digits(final int len) {
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append('1');
        }
        return sb.toString();
    }

    // [databind#4435]
    @Test
    public void testNumberStartingWithDot() throws Exception {
        _testNumberWith(".555555555555555555555555555555");
        _testNumberWith("-.555555555555555555555555555555");
        _testNumberWith("+.555555555555555555555555555555");
    }

    // [databind#4577]
    @Test
    public void testNumberEndingWithDot() throws Exception {
        _testNumberWith("55.");
        _testNumberWith("-55.");
        _testNumberWith("+55.");
    }
    
    private void _testNumberWith(String num) throws Exception
    {
        BigDecimal exp = new BigDecimal(num);
        BigDecimalWrapper w = MAPPER.readValue("{\"number\":\"" + num + "\"}", BigDecimalWrapper.class);
        assertEquals(exp, w.number);
    }

    private String generateJson(final String fieldName) {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"")
                .append(fieldName)
                .append("\": ");
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        sb.append("}");
        return sb.toString();
    }
}

package com.fasterxml.jackson.databind.deser.lazy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.io.NumberInput;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.*;

import java.math.BigDecimal;

/**
 * Tests to verify that skipping of unknown/unmapped works such that
 * "expensive" numbers (all floating-point, {@code BigInteger}) is avoided.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.fasterxml.jackson.core.io.NumberInput")
public class LazyIgnoralForNumbers3730Test extends BaseMapTest
{
    static class ExtractFieldsNoDefaultConstructor3730 {
        private final String s;
        private final int i;

        @JsonCreator
        public ExtractFieldsNoDefaultConstructor3730(@JsonProperty("s") String s, @JsonProperty("i") int i) {
            this.s = s;
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public int getI() {
            return i;
        }
    }

    // Another class to test that we do actually call parse method -- just not
    // eagerly. But MUST use "@JsonUnwrapped" to force buffering; creator not enough
    static class UnwrappedWithNumber {
        @JsonUnwrapped
        public Values values;

        static class Values {
            public String s;
            public int i;
            public Number n;
        }
    }

    // Same as above
    static class UnwrappedWithBigDecimal {
        @JsonUnwrapped
        public Values values;

        static class Values {
            public String s;
            public int i;
            public BigDecimal n;
        }
    }

    // And same here
    static class UnwrappedWithDouble {
        @JsonUnwrapped
        public Values values;

        static class Values {
            public String s;
            public int i;
            public double n;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final ObjectMapper STRICT_MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public void testIgnoreBigInteger() throws Exception
    {
        final String MOCK_MSG = "mock: deliberate failure for parseBigInteger";
        mockStatic(NumberInput.class);
        when(NumberInput.parseBigInteger(Mockito.anyString()))
                .thenThrow(new IllegalStateException(MOCK_MSG));
        when(NumberInput.parseBigInteger(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new IllegalStateException(MOCK_MSG));
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 999; i++) {
            stringBuilder.append(7);
        }
        final String testBigInteger = stringBuilder.toString();
        final String json = genJson(testBigInteger);
        ExtractFieldsNoDefaultConstructor3730 ef =
                MAPPER.readValue(json, ExtractFieldsNoDefaultConstructor3730.class);
        assertNotNull(ef);
        // Ok but then let's ensure method IS called, if field is actually mapped,
        // first to Number
        try {
            Object ob = STRICT_MAPPER.readValue(json, UnwrappedWithNumber.class);
            fail("Should throw exception with mocking: instead got: "+MAPPER.writeValueAsString(ob));
        } catch (DatabindException e) {
            verifyMockException(e, MOCK_MSG);
        }
    }

    public void testIgnoreFPValuesDefault() throws Exception
    {
        final String MOCK_MSG = "mock: deliberate failure for parseDouble";
        // With default settings we would parse Doubles, so check
        mockStatic(NumberInput.class);
        when(NumberInput.parseDouble(Mockito.anyString()))
                .thenThrow(new IllegalStateException(MOCK_MSG));
        when(NumberInput.parseDouble(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new IllegalStateException(MOCK_MSG));
        final String json = genJson("0.25");
        ExtractFieldsNoDefaultConstructor3730 ef =
                MAPPER.readValue(json, ExtractFieldsNoDefaultConstructor3730.class);
        assertNotNull(ef);

        // Ok but then let's ensure method IS called, if field is actually mapped
        // First, to "Number"
        try {
            STRICT_MAPPER.readValue(json, UnwrappedWithNumber.class);
            fail("Should throw exception with mocking!");
        } catch (DatabindException e) {
            verifyMockException(e, MOCK_MSG);
        }

        // And then to "double"
        // 01-Feb-2023, tatu: Not quite working, yet:
        try {
            STRICT_MAPPER.readValue(json, UnwrappedWithDouble.class);
            fail("Should throw exception with mocking!");
        } catch (DatabindException e) {
            e.printStackTrace();
            verifyMockException(e, MOCK_MSG);
        }
    }

    public void testIgnoreFPValuesBigDecimal() throws Exception
    {
        final String MOCK_MSG = "mock: deliberate failure for parseBigDecimal";
        ObjectReader reader = MAPPER
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readerFor(ExtractFieldsNoDefaultConstructor3730.class);

        // Now should get calls to `parseBigDecimal`... eventually
        mockStatic(NumberInput.class);
        when(NumberInput.parseBigDecimal(Mockito.anyString()))
                .thenThrow(new IllegalStateException(MOCK_MSG));
        when(NumberInput.parseBigDecimal(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new IllegalStateException(MOCK_MSG));

        final String json = genJson("0.25");
        ExtractFieldsNoDefaultConstructor3730 ef =
                reader.readValue(genJson(json));
        assertNotNull(ef);

        // But then ensure we'll fail with unknown (except does it work with unwrapped?)
        reader = reader.with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Ok but then let's ensure method IS called, if field is actually mapped
        // First to Number
        try {
            reader.forType(UnwrappedWithNumber.class).readValue(json);
            fail("Should throw exception with mocking!");
        } catch (DatabindException e) {
            verifyMockException(e, MOCK_MSG);
        }

        // And then to "BigDecimal"
        // 01-Feb-2023, tatu: Not quite working, yet:
        try {
            reader.forType(UnwrappedWithBigDecimal.class).readValue(json);
            fail("Should throw exception with mocking!");
        } catch (DatabindException e) {
            verifyMockException(e, MOCK_MSG);
        }
    }

    private String genJson(String num) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("{\"s\":\"s\",\"n\":")
                .append(num)
                .append(",\"i\":1}");
        return stringBuilder.toString();
    }

    private void verifyMockException(DatabindException e, String expMsg) {
        Throwable cause = e.getCause();
        assertEquals(IllegalStateException.class, cause.getClass());
        assertEquals(expMsg, cause.getMessage());
    }
}

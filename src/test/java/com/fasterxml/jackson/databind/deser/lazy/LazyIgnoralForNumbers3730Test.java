package com.fasterxml.jackson.databind.deser.lazy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.core.io.NumberInput;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.*;

/**
 * Tests to verify that skipping of unknown/unmapped works such that
 * "expensive" numbers (all floating-point, {@code BigInteger} is avoided.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.fasterxml.jackson.core.io.NumberInput")
public class LazyIgnoralForNumbers3730Test extends BaseMapTest
{
    static class ExtractFieldsNoDefaultConstructor {
        private final String s;
        private final int i;

        @JsonCreator
        public ExtractFieldsNoDefaultConstructor(@JsonProperty("s") String s, @JsonProperty("i") int i) {
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    
    public void testIgnoreBigInteger() throws Exception
    {
        mockStatic(NumberInput.class);
        when(NumberInput.parseBigInteger(Mockito.anyString()))
                .thenThrow(new IllegalStateException("mock: deliberate failure"));
        when(NumberInput.parseBigInteger(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new IllegalStateException("mock: deliberate failure"));
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 999; i++) {
            stringBuilder.append(7);
        }
        final String testBigInteger = stringBuilder.toString();
        ExtractFieldsNoDefaultConstructor ef =
                MAPPER.readValue(genJson(testBigInteger), ExtractFieldsNoDefaultConstructor.class);
        assertNotNull(ef);
    }

    private String genJson(String num) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("{\"s\":\"s\",\"n\":")
                .append(num)
                .append(",\"i\":1}");
        return stringBuilder.toString();
    }
}

package com.fasterxml.jackson.databind.deser.dos;

import java.util.Date;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

public class DeepArrayWrappingForDeser3590Test
{
    // 05-Sep-2022, tatu: Before fix, failed with 5000
    private final static int TOO_DEEP_NESTING = 9999;

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            .build();

    private final static String TOO_DEEP_DOC = _nestedDoc(TOO_DEEP_NESTING, "[ ", "] ", "123");

    @Test
    public void testArrayWrappingForBoolean() throws Exception
    {
        _testArrayWrappingFor(Boolean.class);
        _testArrayWrappingFor(Boolean.TYPE);
    }

    @Test
    public void testArrayWrappingForByte() throws Exception
    {
        _testArrayWrappingFor(Byte.class);
        _testArrayWrappingFor(Byte.TYPE);
    }

    @Test
    public void testArrayWrappingForShort() throws Exception
    {
        _testArrayWrappingFor(Short.class);
        _testArrayWrappingFor(Short.TYPE);
    }

    @Test
    public void testArrayWrappingForInt() throws Exception
    {
        _testArrayWrappingFor(Integer.class);
        _testArrayWrappingFor(Integer.TYPE);
    }

    @Test
    public void testArrayWrappingForLong() throws Exception
    {
        _testArrayWrappingFor(Long.class);
        _testArrayWrappingFor(Long.TYPE);
    }

    @Test
    public void testArrayWrappingForFloat() throws Exception
    {
        _testArrayWrappingFor(Float.class);
        _testArrayWrappingFor(Float.TYPE);
    }

    @Test
    public void testArrayWrappingForDouble() throws Exception
    {
        _testArrayWrappingFor(Double.class);
        _testArrayWrappingFor(Double.TYPE);
    }

    @Test
    public void testArrayWrappingForDate() throws Exception
    {
        _testArrayWrappingFor(Date.class);
    }

    private void _testArrayWrappingFor(Class<?> cls) throws Exception
    {
        try {
            MAPPER.readValue(TOO_DEEP_DOC, cls);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
            verifyException(e, "nested Arrays not allowed");
        }
    }

    private static String _nestedDoc(int nesting, String open, String close, String content) {
        StringBuilder sb = new StringBuilder(nesting * (open.length() + close.length()));
        for (int i = 0; i < nesting; ++i) {
            sb.append(open);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        sb.append("\n").append(content).append("\n");
        for (int i = 0; i < nesting; ++i) {
            sb.append(close);
            if ((i & 31) == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}

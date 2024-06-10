package com.fasterxml.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// [databind#4071]: Ignore "message" for custom exceptions with only default constructor
@SuppressWarnings("serial")
public class CustomExceptionDeser4071Test
{
    static class CustomThrowable4071 extends Throwable { }
    
    static class CustomRuntimeException4071 extends RuntimeException { }
    
    static class CustomCheckedException4071 extends Exception { }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testCustomException() throws Exception
    {
        String exStr = MAPPER.writeValueAsString(new CustomThrowable4071());
        assertNotNull(MAPPER.readValue(exStr, CustomThrowable4071.class));
    }

    @Test
    public void testCustomRuntimeException() throws Exception
    {
        String exStr = MAPPER.writeValueAsString(new CustomRuntimeException4071());
        assertNotNull(MAPPER.readValue(exStr, CustomRuntimeException4071.class));
    }

    @Test
    public void testCustomCheckedException() throws Exception
    {
        String exStr = MAPPER.writeValueAsString(new CustomCheckedException4071());
        assertNotNull(MAPPER.readValue(exStr, CustomCheckedException4071.class));
    }

    @Test
    public void testDeserAsThrowable() throws Exception
    {
        _testDeserAsThrowable(MAPPER.writeValueAsString(new CustomRuntimeException4071()));
        _testDeserAsThrowable(MAPPER.writeValueAsString(new CustomCheckedException4071()));
        _testDeserAsThrowable(MAPPER.writeValueAsString(new CustomThrowable4071()));
    }

    private void _testDeserAsThrowable(String exStr) throws Exception
    {
        assertNotNull(MAPPER.readValue(exStr, Throwable.class));
    }
}

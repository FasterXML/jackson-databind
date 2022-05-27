package com.fasterxml.jackson.databind.exc;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Unit tests for verifying that simple exceptions can be deserialized.
 */
public class ExceptionDeserializationTest
    extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class MyException extends Exception
    {
        protected int value;

        protected String myMessage;
        protected HashMap<String,Object> stuff = new HashMap<String, Object>();

        @JsonCreator
        MyException(@JsonProperty("message") String msg, @JsonProperty("value") int v)
        {
            super(msg);
            myMessage = msg;
            value = v;
        }

        public int getValue() { return value; }

        public String getFoo() { return "bar"; }

        @JsonAnySetter public void setter(String key, Object value)
        {
            stuff.put(key, value);
        }
    }

    @SuppressWarnings("serial")
    static class MyNoArgException extends Exception
    {
        @JsonCreator MyNoArgException() { }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testIOException() throws IOException
    {
        IOException ioe = new IOException("TEST");
        String json = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(ioe);
        IOException result = MAPPER.readValue(json, IOException.class);
        assertEquals(ioe.getMessage(), result.getMessage());
    }

    public void testWithCreator() throws IOException
    {
        final String MSG = "the message";
        String json = MAPPER.writeValueAsString(new MyException(MSG, 3));

        MyException result = MAPPER.readValue(json, MyException.class);
        assertEquals(MSG, result.getMessage());
        assertEquals(3, result.value);

        // 27-May-2022, tatu: With [databind#3497] we actually get 3, not 1
        //    "extra" things exposed
        assertEquals(3, result.stuff.size());
        assertEquals(result.getFoo(), result.stuff.get("foo"));
        assertEquals("the message", result.stuff.get("localizedMessage"));
        assertTrue(result.stuff.containsKey("suppressed"));
    }

    public void testWithNullMessage() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String json = mapper.writeValueAsString(new IOException((String) null));
        IOException result = mapper.readValue(json, IOException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
    }

    public void testNoArgsException() throws IOException
    {
        MyNoArgException exc = MAPPER.readValue("{}", MyNoArgException.class);
        assertNotNull(exc);
    }

    // try simulating JDK 7 behavior
    public void testJDK7SuppressionProperty() throws IOException
    {
        Exception exc = MAPPER.readValue("{\"suppressed\":[]}", IOException.class);
        assertNotNull(exc);
    }

    // [databind#381]
    public void testSingleValueArrayDeserialization() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        final IOException exp;
        try {
            throw new IOException("testing");
        } catch (IOException internal) {
            exp = internal;
        }
        final String value = "[" + mapper.writeValueAsString(exp) + "]";

        final IOException cloned = mapper.readValue(value, IOException.class);
        assertEquals(exp.getMessage(), cloned.getMessage());

        _assertEquality(exp.getStackTrace(), cloned.getStackTrace());
    }

    public void testExceptionCauseDeserialization() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        final IOException exp = new IOException("the outer exception", new Throwable("the cause"));

        final String value = mapper.writeValueAsString(exp);
        final IOException act = mapper.readValue(value, IOException.class);

        assertNotNull(act.getCause());
        assertEquals(exp.getCause().getMessage(), act.getCause().getMessage());
        _assertEquality(exp.getCause().getStackTrace(), act.getCause().getStackTrace());
    }


    public void testSuppressedGenericThrowableDeserialization() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        final IOException exp = new IOException("the outer exception");
        exp.addSuppressed(new Throwable("the suppressed exception"));

        final String value = mapper.writeValueAsString(exp);
        final IOException act = mapper.readValue(value, IOException.class);

        assertNotNull(act.getSuppressed());
        assertEquals(1, act.getSuppressed().length);
        assertEquals(exp.getSuppressed()[0].getMessage(), act.getSuppressed()[0].getMessage());
        _assertEquality(exp.getSuppressed()[0].getStackTrace(), act.getSuppressed()[0].getStackTrace());
    }

    public void testSuppressedTypedExceptionDeserialization() throws IOException
    {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .allowIfSubType(Throwable.class)
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL)
                .build();

        final IOException exp = new IOException("the outer exception");
        exp.addSuppressed(new IllegalArgumentException("the suppressed exception"));

        final String value = mapper.writeValueAsString(exp);
        final IOException act = mapper.readValue(value, IOException.class);

        assertNotNull(act.getSuppressed());
        assertEquals(1, act.getSuppressed().length);
        assertEquals(IllegalArgumentException.class, act.getSuppressed()[0].getClass());
        assertEquals(exp.getSuppressed()[0].getMessage(), act.getSuppressed()[0].getMessage());
        _assertEquality(exp.getSuppressed()[0].getStackTrace(), act.getSuppressed()[0].getStackTrace());
    }

    private void _assertEquality(StackTraceElement[] exp, StackTraceElement[] act) {
        assertEquals(exp.length, act.length);
        for (int i = 0; i < exp.length; i++) {
            _assertEquality(i, exp[i], act[i]);
        }
    }

    protected void _assertEquality(int ix, StackTraceElement exp, StackTraceElement act)
    {
        _assertEquality(ix, "className", exp.getClassName(), act.getClassName());
        _assertEquality(ix, "methodName", exp.getMethodName(), act.getMethodName());
        _assertEquality(ix, "fileName", exp.getFileName(), act.getFileName());
        _assertEquality(ix, "lineNumber", exp.getLineNumber(), act.getLineNumber());
    }

    protected void _assertEquality(int ix, String prop,
            Object exp, Object act)
    {
        if (exp == null) {
            if (act == null) {
                return;
            }
        } else {
            if (exp.equals(act)) {
                return;
            }
        }
        fail(String.format("StackTraceElement #%d, property '%s' differs: expected %s, actual %s",
                ix, prop, exp, act));
    }

    public void testSingleValueArrayDeserializationException() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        final IOException exp;
        try {
            throw new IOException("testing");
        } catch (IOException internal) {
            exp = internal;
        }
        final String value = "[" + mapper.writeValueAsString(exp) + "]";

        try {
            mapper.readValue(value, IOException.class);
            fail("Exception not thrown when attempting to deserialize an IOException wrapped in a single value array with UNWRAP_SINGLE_VALUE_ARRAYS disabled");
        } catch (MismatchedInputException exp2) {
            verifyException(exp2, "from Array value (token `JsonToken.START_ARRAY`)");
        }
    }

    // mostly to help with XML module (and perhaps CSV)
    public void testLineNumberAsString() throws IOException
    {
        Exception exc = MAPPER.readValue(a2q(
                "{'message':'Test',\n'stackTrace': "
                +"[ { 'lineNumber':'50' } ] }"
        ), IOException.class);
        assertNotNull(exc);
    }

    // [databind#1842]
    public void testNullAsMessage() throws IOException
    {
        Exception exc = MAPPER.readValue(a2q(
                "{'message':null, 'localizedMessage':null }"
        ), IOException.class);
        assertNotNull(exc);
        assertNull(exc.getMessage());
        assertNull(exc.getLocalizedMessage());
    }

    // [databind#3497]: round-trip with naming strategy
    public void testRoundtripWithoutNamingStrategy() throws Exception
    {
        _testRoundtripWith(MAPPER);
    }

    public void testRoundtripWithNamingStrategy() throws Exception
    {
        final ObjectMapper renamingMapper = JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
                .build();
        _testRoundtripWith(renamingMapper);
    }

    private void _testRoundtripWith(ObjectMapper mapper) throws Exception
    {
        Exception root = new Exception("Root cause");
        Exception leaf = new Exception("Leaf message", root);

        final String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(leaf);
        Exception result = mapper.readValue(json, Exception.class);

        assertEquals(leaf.getMessage(), result.getMessage());
        assertNotNull(result.getCause());
        assertEquals(root.getMessage(), result.getCause().getMessage());
    }
}

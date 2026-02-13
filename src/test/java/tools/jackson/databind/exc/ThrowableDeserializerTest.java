package tools.jackson.databind.exc;

import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Throwable/Exception deserialization, including
 * {@link tools.jackson.databind.deser.jdk.ThrowableDeserializer}.
 */
public class ThrowableDeserializerTest extends DatabindTestUtil
{
    // Exception with default constructor only
    @SuppressWarnings("serial")
    static class DefaultCtorException extends Exception {
        public DefaultCtorException() { super(); }
    }

    // Exception with both default and string constructors
    @SuppressWarnings("serial")
    static class DualCtorException extends Exception {
        public DualCtorException() { super(); }
        public DualCtorException(String msg) { super(msg); }
    }

    // Exception with custom property via @JsonCreator
    @SuppressWarnings("serial")
    static class CustomPropException extends Exception {
        private int code;

        @JsonCreator
        public CustomPropException(@JsonProperty("message") String msg,
                                   @JsonProperty("code") int code)
        {
            super(msg);
            this.code = code;
        }

        public int getCode() { return code; }
    }

    // Exception using @JsonAnySetter (field-based)
    @SuppressWarnings("serial")
    static class AnySetterException extends Exception {
        @JsonAnySetter
        public Map<String, Object> extra = new LinkedHashMap<>();

        public AnySetterException() { super(); }
        public AnySetterException(String msg) { super(msg); }
    }

    // Exception with @JsonCreator, @JsonAnySetter and extra props
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

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Tests for basic deserialization
    /**********************************************************
     */

    @Test
    public void testSimpleIOException() throws Exception
    {
        IOException result = MAPPER.readValue(
                a2q("{'message':'test error'}"), IOException.class);
        assertNotNull(result);
        assertEquals("test error", result.getMessage());
    }

    @Test
    public void testIOExceptionRoundTrip() throws Exception
    {
        IOException orig = new IOException("round-trip test");
        String json = MAPPER.writeValueAsString(orig);
        IOException result = MAPPER.readValue(json, IOException.class);
        assertEquals(orig.getMessage(), result.getMessage());
    }

    @Test
    public void testDefaultCtorException() throws Exception
    {
        DefaultCtorException result = MAPPER.readValue(
                a2q("{}"), DefaultCtorException.class);
        assertNotNull(result);
    }

    @Test
    public void testDefaultCtorExceptionWithMessage() throws Exception
    {
        // When there's no String constructor, message is ignored (or skipped)
        DefaultCtorException result = MAPPER.readValue(
                a2q("{'message':'ignored'}"), DefaultCtorException.class);
        assertNotNull(result);
        // No string constructor, so message won't be set
        assertNull(result.getMessage());
    }

    @Test
    public void testDualCtorWithMessage() throws Exception
    {
        DualCtorException result = MAPPER.readValue(
                a2q("{'message':'dual ctor test'}"), DualCtorException.class);
        assertNotNull(result);
        assertEquals("dual ctor test", result.getMessage());
    }

    @Test
    public void testDualCtorWithoutMessage() throws Exception
    {
        DualCtorException result = MAPPER.readValue(
                a2q("{}"), DualCtorException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
    }

    @Test
    public void testNullMessage() throws Exception
    {
        IOException result = MAPPER.readValue(
                a2q("{'message':null}"), IOException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
    }

    // [databind#1842]
    @Test
    public void testNullAsMessageAndLocalizedMessage() throws Exception
    {
        Exception exc = MAPPER.readValue(a2q(
                "{'message':null, 'localizedMessage':null }"
        ), IOException.class);
        assertNotNull(exc);
        assertNull(exc.getMessage());
        assertNull(exc.getLocalizedMessage());
    }

    @Test
    public void testWithNullMessageNonNullInclusion() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
        String json = mapper.writeValueAsString(new IOException((String) null));
        IOException result = mapper.readValue(json, IOException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
    }

    @Test
    public void testNoArgsException() throws Exception
    {
        MyNoArgException exc = MAPPER.readValue("{}", MyNoArgException.class);
        assertNotNull(exc);
    }

    // try simulating JDK 7 behavior
    @Test
    public void testJDK7SuppressionProperty() throws Exception
    {
        Exception exc = MAPPER.readValue("{\"suppressed\":[]}", IOException.class);
        assertNotNull(exc);
    }

    // mostly to help with XML module (and perhaps CSV)
    @Test
    public void testLineNumberAsString() throws Exception
    {
        Exception exc = MAPPER.readValue(a2q(
                "{'message':'Test',\n'stackTrace': "
                +"[ { 'lineNumber':'50' } ] }"
        ), IOException.class);
        assertNotNull(exc);
    }

    /*
    /**********************************************************
    /* Tests for cause and suppressed
    /**********************************************************
     */

    @Test
    public void testCauseDeserialization() throws Exception
    {
        String json = a2q("{'message':'outer','cause':{'message':'inner'}}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("outer", result.getMessage());
        assertNotNull(result.getCause());
        assertEquals("inner", result.getCause().getMessage());
    }

    @Test
    public void testCauseDeserializationRoundTrip() throws Exception
    {
        final IOException exp = new IOException("the outer exception", new Throwable("the cause"));

        final String value = MAPPER.writeValueAsString(exp);
        final IOException act = MAPPER.readValue(value, IOException.class);

        assertNotNull(act.getCause());
        assertEquals(exp.getCause().getMessage(), act.getCause().getMessage());
        _assertEquality(exp.getCause().getStackTrace(), act.getCause().getStackTrace());
    }

    @Test
    public void testNullCauseDeserialization() throws Exception
    {
        // [databind#4248]: null cause should not blow up
        String json = a2q("{'message':'test','cause':null}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("test", result.getMessage());
    }

    @Test
    public void testSuppressedDeserialization() throws Exception
    {
        String json = a2q("{'message':'main','suppressed':[{'message':'suppressed1'},{'message':'suppressed2'}]}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("main", result.getMessage());
        assertEquals(2, result.getSuppressed().length);
        assertEquals("suppressed1", result.getSuppressed()[0].getMessage());
        assertEquals("suppressed2", result.getSuppressed()[1].getMessage());
    }

    @Test
    public void testSuppressedGenericThrowableDeserialization() throws Exception
    {
        final IOException exp = new IOException("the outer exception");
        exp.addSuppressed(new Throwable("the suppressed exception"));

        final String value = MAPPER.writeValueAsString(exp);
        final IOException act = MAPPER.readValue(value, IOException.class);

        assertNotNull(act.getSuppressed());
        assertEquals(1, act.getSuppressed().length);
        assertEquals(exp.getSuppressed()[0].getMessage(), act.getSuppressed()[0].getMessage());
        _assertEquality(exp.getSuppressed()[0].getStackTrace(), act.getSuppressed()[0].getStackTrace());
    }

    @Test
    public void testSuppressedTypedExceptionDeserialization() throws Exception
    {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray()
                .allowIfSubType(Throwable.class)
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .activateDefaultTyping(typeValidator, DefaultTyping.NON_FINAL)
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

    @Test
    public void testNullSuppressedArray() throws Exception
    {
        String json = a2q("{'message':'test','suppressed':null}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals(0, result.getSuppressed().length);
    }

    // Found by OSS-Fuzz: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=65042
    @Test
    public void testSuppressedWithNullEntries() throws Exception
    {
        String json = a2q("{'message':'test','suppressed':[null]}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        // null entries in suppressed array should be skipped
        assertEquals(0, result.getSuppressed().length);
    }

    @Test
    public void testSuppressedRoundTrip() throws Exception
    {
        IOException orig = new IOException("the outer");
        orig.addSuppressed(new RuntimeException("supp1"));
        orig.addSuppressed(new IllegalArgumentException("supp2"));

        String json = MAPPER.writeValueAsString(orig);
        IOException result = MAPPER.readValue(json, IOException.class);

        assertNotNull(result);
        assertEquals(orig.getMessage(), result.getMessage());
        assertEquals(2, result.getSuppressed().length);
    }

    @Test
    public void testEmptySuppressedArray() throws Exception
    {
        String json = a2q("{'message':'test','suppressed':[]}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals(0, result.getSuppressed().length);
    }

    /*
    /**********************************************************
    /* Tests for @JsonCreator and custom props
    /**********************************************************
     */

    @Test
    public void testCustomPropException() throws Exception
    {
        String json = a2q("{'message':'custom error','code':42}");
        CustomPropException result = MAPPER.readValue(json, CustomPropException.class);
        assertNotNull(result);
        assertEquals("custom error", result.getMessage());
        assertEquals(42, result.getCode());
    }

    @Test
    public void testWithCreator() throws Exception
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

    /*
    /**********************************************************
    /* Tests for @JsonAnySetter
    /**********************************************************
     */

    @Test
    public void testAnySetterException() throws Exception
    {
        // [databind#4316]: any setter should work after message is resolved
        String json = a2q("{'message':'any test','extraField':'extraVal'}");
        AnySetterException result = MAPPER.readValue(json, AnySetterException.class);
        assertNotNull(result);
        assertEquals("any test", result.getMessage());
        assertTrue(result.extra.containsKey("extraField"));
        assertEquals("extraVal", result.extra.get("extraField"));
    }

    @Test
    public void testAnySetterWithoutMessage() throws Exception
    {
        String json = a2q("{'unknownProp':'val'}");
        AnySetterException result = MAPPER.readValue(json, AnySetterException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
        assertTrue(result.extra.containsKey("unknownProp"));
    }

    /*
    /**********************************************************
    /* Tests for localizedMessage handling
    /**********************************************************
     */

    @Test
    public void testLocalizedMessageIgnored() throws Exception
    {
        String json = a2q("{'message':'the msg','localizedMessage':'localized ignored'}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("the msg", result.getMessage());
    }

    /*
    /**********************************************************
    /* Tests for message ordering edge cases
    /**********************************************************
     */

    @Test
    public void testPropertiesBeforeMessage() throws Exception
    {
        // Properties coming before "message" should be deferred and applied after construction
        String json = a2q("{'cause':{'message':'cause msg'},'message':'outer msg'}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("outer msg", result.getMessage());
        assertNotNull(result.getCause());
        assertEquals("cause msg", result.getCause().getMessage());
    }

    /*
    /**********************************************************
    /* Tests for stackTrace handling [databind#5674]
    /**********************************************************
     */

    @Test
    public void testNullStackTrace() throws Exception
    {
        // stackTrace with null should not blow up (setStackTrace(null) throws NPE)
        String json = a2q("{'message':'test','stackTrace':null}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("test", result.getMessage());
        // Default stack trace should be preserved
        assertNotNull(result.getStackTrace());
    }

    @Test
    public void testStackTraceDeserialization() throws Exception
    {
        String json = a2q("{'message':'test','stackTrace':[" +
                "{'className':'com.example.Test','methodName':'testMethod'," +
                "'fileName':'Test.java','lineNumber':42}]}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("test", result.getMessage());
        assertNotNull(result.getStackTrace());
        assertEquals(1, result.getStackTrace().length);
        assertEquals("com.example.Test", result.getStackTrace()[0].getClassName());
        assertEquals("testMethod", result.getStackTrace()[0].getMethodName());
        assertEquals("Test.java", result.getStackTrace()[0].getFileName());
        assertEquals(42, result.getStackTrace()[0].getLineNumber());
    }

    @Test
    public void testNullStackTraceBeforeMessage() throws Exception
    {
        // stackTrace with null before message should not blow up
        String json = a2q("{'stackTrace':null,'message':'test'}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals("test", result.getMessage());
        // Default stack trace should be preserved
        assertNotNull(result.getStackTrace());
    }

    /*
    /**********************************************************
    /* Tests for single-value array [databind#381]
    /**********************************************************
     */

    @Test
    public void testSingleValueArrayDeserialization() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();
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

    @Test
    public void testSingleValueArrayDeserializationException() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .build();

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

    /*
    /**********************************************************
    /* Tests for naming strategy [databind#3497]
    /**********************************************************
     */

    @Test
    public void testRoundtripWithoutNamingStrategy() throws Exception
    {
        _testRoundtripWith(MAPPER);
    }

    @Test
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

    /*
    /**********************************************************
    /* Tests for duplicate properties [databind#4248]
    /**********************************************************
     */

    @Test
    public void testWithDups() throws Exception
    {
        // NOTE: by default JSON parser does NOT fail on duplicate properties;
        // we only use them to mimic formats like XML where duplicates can occur
        // (or, malicious JSON...)
        final StringBuilder sb = new StringBuilder(100);
        sb.append("{");
        sb.append("'suppressed': null,\n");
        sb.append("'cause': null,\n");
        for (int i = 0; i < 10; ++i) { // just needs to be more than max distinct props
            sb.append("'stackTrace': [],\n");
        }
        sb.append("'message': 'foo',\n");
        sb.append("'localizedMessage': 'bar'\n}");
        IOException exc = MAPPER.readValue(a2q(sb.toString()), IOException.class);
        assertNotNull(exc);
        assertEquals("foo", exc.getLocalizedMessage());
    }

    /*
    /**********************************************************
    /* Tests for custom exceptions with default ctor [databind#4071]
    /**********************************************************
     */

    @SuppressWarnings("serial")
    static class CustomThrowable4071 extends Throwable { }

    @SuppressWarnings("serial")
    static class CustomRuntimeException4071 extends RuntimeException { }

    @SuppressWarnings("serial")
    static class CustomCheckedException4071 extends Exception { }

    // [databind#4071]: Ignore "message" for custom exceptions with only default constructor
    @Test
    public void testCustomExceptionDefaultCtorRoundTrip() throws Exception
    {
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomThrowable4071()), CustomThrowable4071.class));
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomRuntimeException4071()), CustomRuntimeException4071.class));
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomCheckedException4071()), CustomCheckedException4071.class));
    }

    // [databind#4071]: also verify deserialization as base Throwable type
    @Test
    public void testCustomExceptionDeserAsThrowable() throws Exception
    {
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomRuntimeException4071()), Throwable.class));
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomCheckedException4071()), Throwable.class));
        assertNotNull(MAPPER.readValue(
                MAPPER.writeValueAsString(new CustomThrowable4071()), Throwable.class));
    }

    /*
    /**********************************************************
    /* Tests for subclassed Throwable with @JsonCreator [databind#4827]
    /**********************************************************
     */

    @SuppressWarnings("serial")
    static class SubclassedExceptionWithCause extends Exception {
        @JsonCreator
        public SubclassedExceptionWithCause(
                @JsonProperty("message") String message,
                @JsonProperty("cause") Throwable cause
        ) {
            super(message, cause);
        }
    }

    // [databind#4827]: Subclassed Throwable deserialization with message+cause creator
    @Test
    public void testSubclassedExceptionWithCauseCreator() throws Exception
    {
        final ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        SubclassedExceptionWithCause input = new SubclassedExceptionWithCause(
                "Test Message", new RuntimeException("test runtime cause"));

        String serialized = mapper.writeValueAsString(input);
        SubclassedExceptionWithCause deserialized = mapper.readValue(serialized, SubclassedExceptionWithCause.class);

        assertEquals(input.getMessage(), deserialized.getMessage());
        assertEquals(input.getCause().getMessage(), deserialized.getCause().getMessage());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

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
}

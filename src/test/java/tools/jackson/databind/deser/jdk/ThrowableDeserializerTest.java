package tools.jackson.databind.deser.jdk;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ThrowableDeserializer}.
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

    // Exception with custom property
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

    // Exception using @JsonAnySetter
    @SuppressWarnings("serial")
    static class AnySetterException extends Exception {
        @JsonAnySetter
        public java.util.Map<String, Object> extra = new java.util.LinkedHashMap<>();

        public AnySetterException() { super(); }
        public AnySetterException(String msg) { super(msg); }
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
    public void testNullSuppressedArray() throws Exception
    {
        String json = a2q("{'message':'test','suppressed':null}");
        IOException result = MAPPER.readValue(json, IOException.class);
        assertNotNull(result);
        assertEquals(0, result.getSuppressed().length);
    }

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
    /* Tests for stackTrace handling
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
}

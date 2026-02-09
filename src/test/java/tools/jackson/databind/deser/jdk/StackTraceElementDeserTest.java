package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StackTraceElementDeserializer}.
 */
public class StackTraceElementDeserTest extends DatabindTestUtil
{
    static class StackTraceHolder {
        public StackTraceElement[] trace;
    }

    // [databind#429]: mix-in that renames StackTraceElement properties
    abstract static class StackTraceElementMixIn {
        @JsonProperty("class")
        public abstract String getClassName();

        @JsonProperty("method")
        public abstract String getMethodName();

        @JsonProperty("file")
        public abstract String getFileName();

        @JsonProperty("line")
        public abstract int getLineNumber();
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /*
    /**********************************************************************
    /* Tests for basic deserialization
    /**********************************************************************
     */

    @Test
    public void testBasicStackTraceElement() throws Exception
    {
        String json = a2q("{'className':'com.example.Foo','methodName':'doStuff',"
                + "'fileName':'Foo.java','lineNumber':42}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("com.example.Foo", result.getClassName());
        assertEquals("doStuff", result.getMethodName());
        assertEquals("Foo.java", result.getFileName());
        assertEquals(42, result.getLineNumber());
    }

    @Test
    public void testWithMinimalFields() throws Exception
    {
        // Only className, methodName, fileName and lineNumber
        String json = a2q("{'className':'MyClass','methodName':'myMethod',"
                + "'fileName':'MyClass.java','lineNumber':10}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("MyClass", result.getClassName());
        assertEquals("myMethod", result.getMethodName());
        assertEquals("MyClass.java", result.getFileName());
        assertEquals(10, result.getLineNumber());
    }

    @Test
    public void testWithEmptyObject() throws Exception
    {
        // All fields use defaults
        StackTraceElement result = MAPPER.readValue("{}", StackTraceElement.class);
        assertNotNull(result);
        assertEquals("", result.getClassName());
        assertEquals("", result.getMethodName());
        assertEquals("", result.getFileName());
        assertEquals(-1, result.getLineNumber());
    }

    /*
    /**********************************************************************
    /* Tests for Java 9+ fields (module info)
    /**********************************************************************
     */

    @Test
    public void testWithModuleInfo() throws Exception
    {
        String json = a2q("{'className':'com.example.Bar','methodName':'run',"
                + "'fileName':'Bar.java','lineNumber':100,"
                + "'moduleName':'my.module','moduleVersion':'2.0',"
                + "'classLoaderName':'app'}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("com.example.Bar", result.getClassName());
        assertEquals("run", result.getMethodName());
        assertEquals("Bar.java", result.getFileName());
        assertEquals(100, result.getLineNumber());
        assertEquals("my.module", result.getModuleName());
        assertEquals("2.0", result.getModuleVersion());
        assertEquals("app", result.getClassLoaderName());
    }

    @Test
    public void testWithNullModuleInfo() throws Exception
    {
        String json = a2q("{'className':'Test','methodName':'test',"
                + "'fileName':'Test.java','lineNumber':1,"
                + "'moduleName':null,'moduleVersion':null,'classLoaderName':null}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertNull(result.getModuleName());
        assertNull(result.getModuleVersion());
        assertNull(result.getClassLoaderName());
    }

    /*
    /**********************************************************************
    /* Tests for round-trip
    /**********************************************************************
     */

    @Test
    public void testRoundTrip() throws Exception
    {
        StackTraceElement orig = new StackTraceElement(
                "appLoader", "java.base", "17.0.1",
                "java.lang.String", "valueOf", "String.java", 3456);
        String json = MAPPER.writeValueAsString(orig);
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertEquals(orig, result);
    }

    @Test
    public void testRoundTripFromActualException() throws Exception
    {
        Exception ex;
        try {
            throw new RuntimeException("test");
        } catch (RuntimeException e) {
            ex = e;
        }
        StackTraceElement[] origTrace = ex.getStackTrace();
        assertNotNull(origTrace);
        assertTrue(origTrace.length > 0);

        // Serialize first element
        StackTraceElement first = origTrace[0];
        String json = MAPPER.writeValueAsString(first);
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);

        assertEquals(first.getClassName(), result.getClassName());
        assertEquals(first.getMethodName(), result.getMethodName());
        assertEquals(first.getFileName(), result.getFileName());
        assertEquals(first.getLineNumber(), result.getLineNumber());
    }

    /*
    /**********************************************************************
    /* Tests for array of StackTraceElements
    /**********************************************************************
     */

    @Test
    public void testStackTraceArray() throws Exception
    {
        String json = a2q("[{'className':'A','methodName':'a','fileName':'A.java','lineNumber':1},"
                + "{'className':'B','methodName':'b','fileName':'B.java','lineNumber':2}]");
        StackTraceElement[] result = MAPPER.readValue(json, StackTraceElement[].class);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("A", result[0].getClassName());
        assertEquals("B", result[1].getClassName());
    }

    @Test
    public void testEmptyStackTraceArray() throws Exception
    {
        StackTraceElement[] result = MAPPER.readValue("[]", StackTraceElement[].class);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testStackTraceInHolder() throws Exception
    {
        String json = a2q("{'trace':["
                + "{'className':'X','methodName':'doX','fileName':'X.java','lineNumber':99}"
                + "]}");
        StackTraceHolder result = MAPPER.readValue(json, StackTraceHolder.class);
        assertNotNull(result);
        assertNotNull(result.trace);
        assertEquals(1, result.trace.length);
        assertEquals("X", result.trace[0].getClassName());
        assertEquals(99, result.trace[0].getLineNumber());
    }

    /*
    /**********************************************************************
    /* Tests for edge cases
    /**********************************************************************
     */

    @Test
    public void testUnknownPropertiesIgnored() throws Exception
    {
        // Adapter class has fields like 'declaringClass', 'nativeMethod', 'format'
        // that don't map to StackTraceElement constructor args directly
        String json = a2q("{'className':'Test','methodName':'m','fileName':'T.java',"
                + "'lineNumber':5,'nativeMethod':true,'declaringClass':'Test',"
                + "'format':'some format'}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("Test", result.getClassName());
    }

    @Test
    public void testLineNumberAsString() throws Exception
    {
        // Some formats may provide lineNumber as a String
        String json = a2q("{'className':'Cls','methodName':'m','fileName':'Cls.java','lineNumber':'77'}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals(77, result.getLineNumber());
    }

    @Test
    public void testSingleValueArrayUnwrap() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(StackTraceElement.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);
        String json = a2q("[{'className':'W','methodName':'wrap','fileName':'W.java','lineNumber':1}]");
        StackTraceElement result = r.readValue(json);
        assertNotNull(result);
        assertEquals("W", result.getClassName());
    }

    /*
    /**********************************************************************
    /* Tests for [databind#429]: mix-in @JsonProperty support
    /**********************************************************************
     */

    // [databind#429]
    @Test
    public void testDeserWithMixInPropertyNames() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(StackTraceElement.class, StackTraceElementMixIn.class)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        String json = a2q("{'class':'com.example.Foo','method':'doStuff',"
                + "'file':'Foo.java','line':42}");
        StackTraceElement result = mapper.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("com.example.Foo", result.getClassName());
        assertEquals("doStuff", result.getMethodName());
        assertEquals("Foo.java", result.getFileName());
        assertEquals(42, result.getLineNumber());
    }

    // [databind#429]
    @Test
    public void testRoundTripWithMixIn() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(StackTraceElement.class, StackTraceElementMixIn.class)
                .build();

        StackTraceElement orig = new StackTraceElement(
                "appLoader", "java.base", "17.0.1",
                "java.lang.String", "valueOf", "String.java", 3456);

        String json = mapper.writeValueAsString(orig);
        // Verify serialization uses mix-in names
        assertTrue(json.contains("\"class\""));
        assertTrue(json.contains("\"method\""));
        assertTrue(json.contains("\"file\""));
        assertTrue(json.contains("\"line\""));

        // Verify deserialization with mix-in names
        StackTraceElement result = mapper.readValue(json, StackTraceElement.class);
        assertEquals(orig.getClassName(), result.getClassName());
        assertEquals(orig.getMethodName(), result.getMethodName());
        assertEquals(orig.getFileName(), result.getFileName());
        assertEquals(orig.getLineNumber(), result.getLineNumber());
    }

    // [databind#429]: ensure standard (no mix-in) deserialization still works
    @Test
    public void testStandardDeserUnaffectedByMixInFeature() throws Exception
    {
        // Use the default mapper without any mix-ins
        String json = a2q("{'className':'com.example.Bar','methodName':'run',"
                + "'fileName':'Bar.java','lineNumber':100}");
        StackTraceElement result = MAPPER.readValue(json, StackTraceElement.class);
        assertNotNull(result);
        assertEquals("com.example.Bar", result.getClassName());
        assertEquals("run", result.getMethodName());
        assertEquals("Bar.java", result.getFileName());
        assertEquals(100, result.getLineNumber());
    }
}

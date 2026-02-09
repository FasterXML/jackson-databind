package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link StackTraceElement} deserialization
 * wrt issue {@code [databind#429]}.
 */
public class StackTraceElementDeser429Test extends DatabindTestUtil
{
    // Mix-in that renames StackTraceElement properties
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
    /* Tests for mix-in @JsonProperty support
    /**********************************************************************
     */

    // [databind#429]
    @JacksonTestFailureExpected
    @Test
    public void testDeserWithMixInPropertyNames() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(StackTraceElement.class, StackTraceElementMixIn.class)
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
    @JacksonTestFailureExpected
    @Test
    public void testRoundTripWithMixIn() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(StackTraceElement.class, StackTraceElementMixIn.class)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
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

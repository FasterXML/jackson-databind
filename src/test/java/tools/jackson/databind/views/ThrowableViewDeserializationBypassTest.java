package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @JsonView} filtering is applied by {@code BeanDeserializer} (and the array/builder
 * variants), but {@link tools.jackson.databind.deser.jdk.ThrowableDeserializer} overrides
 * {@code deserializeFromObject} wholesale and never consulted the active view. For an
 * exception type that reaches that loop (default-constructor, no property-based creator),
 * a view-hidden property was populated from input even when the active view excluded it.
 */
public class ThrowableViewDeserializationBypassTest extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    @SuppressWarnings("serial")
    static class ViewException extends RuntimeException {
        @JsonView(Public.class) public String pub;
        @JsonView(Internal.class) public String sec; // internal-only
        public ViewException() { super(); }
    }

    // Same, but with a single-String constructor so that "message" is actually settable
    // (with only a default constructor it is skipped; see [databind#4071])
    @SuppressWarnings("serial")
    static class StdPropsException extends RuntimeException {
        @JsonView(Public.class) public String pub;
        @JsonView(Internal.class) public String sec; // internal-only
        public StdPropsException() { super(); }
        public StdPropsException(String msg) { super(msg); }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper FAIL_ON_UNEXPECTED_MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES).build();

    // Under the Public view the Internal-only property must not be set from input
    @Test
    public void throwableHonorsViewOnDeserialize() throws Exception {
        ViewException ex = MAPPER.readerWithView(Public.class)
                .forType(ViewException.class)
                .readValue("{\"pub\":\"visible\",\"sec\":\"leaked\"}");

        assertEquals("visible", ex.pub);
        assertNull(ex.sec,
                "view-hidden 'sec' should stay null under the Public view but was: " + ex.sec);
    }

    // Control: with no active view every property is set as before
    @Test
    public void throwableWithoutViewSetsAll() throws Exception {
        ViewException ex = MAPPER.readerFor(ViewException.class)
                .readValue("{\"pub\":\"visible\",\"sec\":\"kept\"}");

        assertEquals("visible", ex.pub);
        assertEquals("kept", ex.sec);
    }

    // [databind#6174]: view filtering must not affect the standard `Throwable` properties;
    // "message", "cause", "stackTrace", "suppressed" and "localizedMessage" have no
    // `@JsonView` of their own and must default to inclusion under any active view.
    @Test
    public void standardThrowablePropsIncludedUnderView() throws Exception {
        final String json = """
{
  "message" : "the message",
  "cause" : { "message" : "root cause" },
  "stackTrace" : [ {
    "className" : "some.Class", "methodName" : "someMethod",
    "fileName" : "Class.java", "lineNumber" : 42
  } ],
  "suppressed" : [ { "message" : "suppressed one" } ],
  "localizedMessage" : "the message",
  "pub" : "visible",
  "sec" : "leaked"
}
""";
        StdPropsException ex = MAPPER.readerWithView(Public.class)
                .forType(StdPropsException.class)
                .readValue(json);

        // First: view filtering still applies to view-annotated properties
        assertEquals("visible", ex.pub);
        assertNull(ex.sec);

        // But none of the standard `Throwable` properties may be dropped:
        assertEquals("the message", ex.getMessage());
        assertEquals("the message", ex.getLocalizedMessage());

        assertNotNull(ex.getCause(), "'cause' should be set under active view");
        assertEquals("root cause", ex.getCause().getMessage());

        // NOTE: only checking that the property itself was applied from input (a
        // single frame), and not left as the multi-frame fill-in trace. Contents of
        // the nested `StackTraceElement` follow the regular bean/View rules -- with
        // `DEFAULT_VIEW_INCLUSION` disabled its un-annotated properties are not part
        // of any view -- which is out of scope here.
        StackTraceElement[] trace = ex.getStackTrace();
        assertEquals(1, trace.length,
                "'stackTrace' should be set from input under active view");

        Throwable[] suppressed = ex.getSuppressed();
        assertEquals(1, suppressed.length,
                "'suppressed' should be set under active view");
        assertEquals("suppressed one", suppressed[0].getMessage());
    }

    // [databind#437]: with `FAIL_ON_UNEXPECTED_VIEW_PROPERTIES` enabled, a property
    // outside the active view is reported as an unexpected property instead of skipped
    @Test
    public void throwableFailsOnUnexpectedViewProperty() throws Exception {
        ObjectReader r = FAIL_ON_UNEXPECTED_MAPPER.readerWithView(Public.class)
                .forType(ViewException.class);
        try {
            r.readValue("{\"pub\":\"visible\",\"sec\":\"leaked\"}");
            fail("should not pass, but fail with exception with unexpected view");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "Property 'sec' is not part of current active view");
        }
    }

    // ...but the standard `Throwable` properties are exempt from view filtering, so
    // they must not trigger the failure either
    @Test
    public void throwableStandardPropsDoNotFailOnUnexpectedView() throws Exception {
        final String json = """
{
  "message" : "the message",
  "cause" : { "message" : "root cause" },
  "stackTrace" : [ ],
  "suppressed" : [ ],
  "localizedMessage" : "the message",
  "pub" : "visible"
}
""";
        StdPropsException ex = FAIL_ON_UNEXPECTED_MAPPER.readerWithView(Public.class)
                .forType(StdPropsException.class)
                .readValue(json);

        assertEquals("visible", ex.pub);
        assertEquals("the message", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(0, ex.getStackTrace().length);
    }

    // [databind#3497]: ...and the exemption must survive a `PropertyNamingStrategy`.
    // With SNAKE_CASE the property is externally named "stack_trace", which no
    // case-insensitive comparison against "stackTrace" can ever match
    @Test
    public void standardThrowablePropsIncludedUnderViewWithNamingStrategy() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        final String json = """
{
  "message" : "the message",
  "cause" : { "message" : "root cause" },
  "stack_trace" : [ {
    "class_name" : "some.Class", "method_name" : "someMethod",
    "file_name" : "Class.java", "line_number" : 42
  } ],
  "pub" : "visible",
  "sec" : "leaked"
}
""";
        StdPropsException ex = mapper.readerWithView(Public.class)
                .forType(StdPropsException.class)
                .readValue(json);

        assertEquals("visible", ex.pub);
        assertNull(ex.sec);
        assertEquals("the message", ex.getMessage());
        assertNotNull(ex.getCause(), "'cause' should be set under active view");
        assertEquals(1, ex.getStackTrace().length,
                "'stack_trace' should be set from input under active view");
    }
}

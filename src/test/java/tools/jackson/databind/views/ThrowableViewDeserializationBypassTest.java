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

    // [databind#6174]: the exemption must cover ALL standard `Throwable` properties,
    // including "message" and "suppressed" -- which a sub-class may make settable, in
    // which case they are bound as regular properties (and not handled separately)
    @SuppressWarnings("serial")
    static class SettableStdPropsException extends RuntimeException {
        protected String _msg;
        public Throwable[] supp;

        public SettableStdPropsException() { super(); }

        @Override public String getMessage() { return _msg; }
        public void setMessage(String msg) { _msg = msg; }
        public void setSuppressed(Throwable[] s) { supp = s; }
    }

    // [databind#6190]: standard `Throwable` properties carrying explicit `@JsonView`
    // must honor that view during deserialization
    @SuppressWarnings("serial")
    static class ExplicitStackTraceException extends RuntimeException {
        public ExplicitStackTraceException() { super(); }

        @Override @JsonView(Internal.class)
        public void setStackTrace(StackTraceElement[] st) { super.setStackTrace(st); }

        @Override @JsonView(Internal.class)
        public StackTraceElement[] getStackTrace() { return super.getStackTrace(); }
    }

    @SuppressWarnings("serial")
    static class ExplicitCauseException extends RuntimeException {
        public ExplicitCauseException() { super(); }

        @Override @JsonView(Internal.class)
        public synchronized Throwable initCause(Throwable cause) { return super.initCause(cause); }

        @Override @JsonView(Internal.class)
        public Throwable getCause() { return super.getCause(); }
    }

    @SuppressWarnings("serial")
    static class SnakeExplicitStackTraceException extends RuntimeException {
        public SnakeExplicitStackTraceException() { super(); }

        @Override @JsonView(Internal.class)
        public void setStackTrace(StackTraceElement[] st) { super.setStackTrace(st); }

        @Override @JsonView(Internal.class)
        public StackTraceElement[] getStackTrace() { return super.getStackTrace(); }
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

    // [databind#6174]: ...and that holds for "message"/"suppressed" bound as regular
    // properties, too: without an explicit `@JsonView` they must not be view-filtered
    @Test
    public void settableStandardThrowablePropsIncludedUnderView() throws Exception {
        final String json = """
{
  "message" : "the message",
  "suppressed" : [ { "message" : "suppressed one" } ]
}
""";
        SettableStdPropsException ex = MAPPER.readerWithView(Public.class)
                .forType(SettableStdPropsException.class)
                .readValue(json);
        assertEquals("the message", ex.getMessage(),
                "settable 'message' should be set from input under active view");
        assertNotNull(ex.supp, "settable 'suppressed' should be set from input under active view");
        assertEquals(1, ex.supp.length);

        // ... and must not trigger `FAIL_ON_UNEXPECTED_VIEW_PROPERTIES` either
        SettableStdPropsException ex2 = FAIL_ON_UNEXPECTED_MAPPER.readerWithView(Public.class)
                .forType(SettableStdPropsException.class)
                .readValue(json);
        assertEquals("the message", ex2.getMessage());
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

    // [databind#6190]: explicit @JsonView on standard Throwable properties must be honored
    @Test
    public void explicitViewOnStackTraceRespected() throws Exception {
        final String json = """
{
  "stackTrace" : [ {
    "className" : "some.Class", "methodName" : "m",
    "fileName" : "C.java", "lineNumber" : 42
  } ]
}
""";
        // Under Public view, stackTrace restricted to Internal should NOT be populated from input
        ExplicitStackTraceException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ExplicitStackTraceException.class)
                .readValue(json);
        assertNotEquals(1, exPublic.getStackTrace().length,
                "stackTrace restricted to Internal must not be populated under Public view");

        // Under Internal view, stackTrace SHOULD be populated from input (1 frame from input)
        ExplicitStackTraceException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ExplicitStackTraceException.class)
                .readValue(json);
        assertEquals(1, exInternal.getStackTrace().length,
                "stackTrace restricted to Internal should be set from input under Internal view");

        // Under no view (full access), stackTrace and its element details SHOULD be populated
        ExplicitStackTraceException exNoView = MAPPER.readerFor(ExplicitStackTraceException.class)
                .readValue(json);
        assertEquals(1, exNoView.getStackTrace().length);
        assertEquals("some.Class", exNoView.getStackTrace()[0].getClassName());
    }

    // [databind#6190]: explicit @JsonView on cause must be honored
    @Test
    public void explicitViewOnCauseRespected() throws Exception {
        final String json = """
{
  "cause" : { "message" : "secret root cause" }
}
""";
        ExplicitCauseException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ExplicitCauseException.class)
                .readValue(json);
        assertNull(exPublic.getCause(),
                "cause restricted to Internal must not be populated under Public view");

        ExplicitCauseException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ExplicitCauseException.class)
                .readValue(json);
        assertNotNull(exInternal.getCause());
        assertEquals("secret root cause", exInternal.getCause().getMessage());
    }

    // [databind#6190]: with FAIL_ON_UNEXPECTED_VIEW_PROPERTIES, an explicit view on a
    // standard property outside the active view must trigger an unexpected-view error
    @Test
    public void explicitViewOnStandardPropsFailsOnUnexpectedView() throws Exception {
        final String json = """
{
  "stackTrace" : [ {
    "className" : "some.Class", "methodName" : "m",
    "fileName" : "C.java", "lineNumber" : 42
  } ]
}
""";
        ObjectReader r = FAIL_ON_UNEXPECTED_MAPPER.readerWithView(Public.class)
                .forType(ExplicitStackTraceException.class);
        try {
            r.readValue(json);
            fail("should not pass, but fail with exception on unexpected view");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "Property 'stackTrace' is not part of current active view");
        }
    }

    // [databind#6190]: explicit view must work with PropertyNamingStrategy
    @Test
    public void explicitViewUnderSnakeCaseNamingStrategy() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        final String json = """
{
  "stack_trace" : [ {
    "class_name" : "some.Class", "method_name" : "m",
    "file_name" : "C.java", "line_number" : 42
  } ]
}
""";
        SnakeExplicitStackTraceException exPublic = mapper.readerWithView(Public.class)
                .forType(SnakeExplicitStackTraceException.class)
                .readValue(json);
        assertNotEquals(1, exPublic.getStackTrace().length,
                "stack_trace restricted to Internal must not be populated under Public view");

        SnakeExplicitStackTraceException exInternal = mapper.readerWithView(Internal.class)
                .forType(SnakeExplicitStackTraceException.class)
                .readValue(json);
        assertEquals(1, exInternal.getStackTrace().length);
    }
}

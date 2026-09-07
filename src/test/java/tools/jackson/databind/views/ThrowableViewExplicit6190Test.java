package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying that explicit {@code @JsonView} annotations placed on standard
 * {@link Throwable} properties (such as "stackTrace" or "cause") are honored during
 * deserialization (see [databind#6190]).
 */
public class ThrowableViewExplicit6190Test extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    // [databind#6190]: "message" and "suppressed" normally have no setter, so they are
    // never bound as regular properties -- an explicit `@JsonView` on them must still
    // be honored by the "unknown name" branches of the read loop
    @SuppressWarnings("serial")
    static class ExplicitMessageException extends RuntimeException {
        public ExplicitMessageException() { super(); }
        public ExplicitMessageException(String msg) { super(msg); }

        @Override @JsonView(Internal.class)
        public String getMessage() { return super.getMessage(); }
    }

    @SuppressWarnings("serial")
    static class SuppressedException extends RuntimeException {
        public SuppressedException() { super(); }
        public SuppressedException(String msg) { super(msg); }
    }

    // `Throwable.getSuppressed()` is `final`, so a mix-in is the way to annotate it
    static abstract class SuppressedViewMixIn {
        @JsonView(Internal.class)
        public abstract Throwable[] getSuppressed();
    }

    // [databind#6190]: a class-level `@JsonView` is explicit intent too -- it covers
    // every property of the class, including the standard `Throwable` ones
    @JsonView(Internal.class)
    @SuppressWarnings("serial")
    static class ClassViewException extends RuntimeException {
        public ClassViewException() { super(); }
        public ClassViewException(String msg) { super(msg); }
    }

    // Same, but bound through a property-based Creator rather than setters
    @JsonView(Internal.class)
    @SuppressWarnings("serial")
    static class ClassViewCreatorException extends RuntimeException {
        @JsonCreator
        public ClassViewCreatorException(@JsonProperty("message") String msg,
                @JsonProperty("cause") Throwable cause) {
            super(msg, cause);
        }
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
    static class ExplicitGetCauseOnlyException extends RuntimeException {
        public ExplicitGetCauseOnlyException() { super(); }

        // Only annotate getCause, leaving initCause unannotated
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

    // [databind#6190]: explicit @JsonView on getter-only cause must also be honored
    @Test
    public void explicitViewOnGetCauseOnlyRespected() throws Exception {
        final String json = """
{
  "cause" : { "message" : "secret root cause" }
}
""";
        ExplicitGetCauseOnlyException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ExplicitGetCauseOnlyException.class)
                .readValue(json);
        assertNull(exPublic.getCause(),
                "cause with @JsonView on getCause only must not be populated under Public view");

        ExplicitGetCauseOnlyException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ExplicitGetCauseOnlyException.class)
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

    // [databind#6190]: the standard-property exemption must not discard a class-level
    // `@JsonView`; it exists for properties defaulted out of every view, not for ones
    // the user explicitly placed in a view
    @Test
    public void classLevelViewRespectedOnStandardProps() throws Exception {
        final String json = """
                {"message":"the msg","cause":{"message":"root"}}""";

        // Whole class is Internal-only, so under Public view "cause" is not set
        ClassViewException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ClassViewException.class).readValue(json);
        assertNull(exPublic.getCause(),
                "class-level @JsonView(Internal) must exclude 'cause' under Public view");

        // ...but under Internal view it is
        ClassViewException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ClassViewException.class).readValue(json);
        assertNotNull(exInternal.getCause());
        assertEquals("root", exInternal.getCause().getMessage());
    }

    // ...and the Creator-bound path must agree with the setter-bound one above
    @Test
    public void classLevelViewRespectedViaCreator() throws Exception {
        final String json = """
                {"message":"the msg","cause":{"message":"root"}}""";

        ClassViewCreatorException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ClassViewCreatorException.class).readValue(json);
        assertNull(exPublic.getCause(),
                "class-level @JsonView(Internal) must exclude Creator-bound 'cause' under Public view");

        ClassViewCreatorException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ClassViewCreatorException.class).readValue(json);
        assertNotNull(exInternal.getCause());
        assertEquals("root", exInternal.getCause().getMessage());
    }

    // [databind#6190]: explicit view on the non-settable "message"
    @Test
    public void explicitViewOnMessageRespected() throws Exception {
        final String json = """
                {"message":"the msg"}""";

        ExplicitMessageException exPublic = MAPPER.readerWithView(Public.class)
                .forType(ExplicitMessageException.class).readValue(json);
        assertNull(exPublic.getMessage(),
                "'message' restricted to Internal must not be set under Public view");

        ExplicitMessageException exInternal = MAPPER.readerWithView(Internal.class)
                .forType(ExplicitMessageException.class).readValue(json);
        assertEquals("the msg", exInternal.getMessage());

        // ...and with no active view at all it is set as before
        ExplicitMessageException exNoView = MAPPER.readerFor(ExplicitMessageException.class)
                .readValue(json);
        assertEquals("the msg", exNoView.getMessage());
    }

    // ...same for the non-settable "suppressed"
    @Test
    public void explicitViewOnSuppressedRespected() throws Exception {
        final String json = """
                {"message":"the msg","suppressed":[{"message":"supp one"}]}""";

        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(SuppressedException.class, SuppressedViewMixIn.class)
                .build();

        SuppressedException exPublic = mapper.readerWithView(Public.class)
                .forType(SuppressedException.class).readValue(json);
        assertEquals(0, exPublic.getSuppressed().length,
                "'suppressed' restricted to Internal must not be set under Public view");
        // "message" carries no view of its own, so it is still applied
        assertEquals("the msg", exPublic.getMessage());

        SuppressedException exInternal = mapper.readerWithView(Internal.class)
                .forType(SuppressedException.class).readValue(json);
        assertEquals(1, exInternal.getSuppressed().length);
        assertEquals("supp one", exInternal.getSuppressed()[0].getMessage());
    }
}

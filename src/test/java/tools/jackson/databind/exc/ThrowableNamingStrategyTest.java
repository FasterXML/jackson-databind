package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#3497]: {@code ThrowableDeserializer} matched the standard {@link Throwable}
 * property names against hard-coded canonical names, using {@code equalsIgnoreCase()}.
 * That absorbs case-changing renames but cannot match snake- or kebab-cased ones, so
 * with such a {@link tools.jackson.databind.PropertyNamingStrategy} the multi-word
 * names ("localizedMessage", "stackTrace") went unrecognized.
 */
public class ThrowableNamingStrategyTest extends DatabindTestUtil
{
    @SuppressWarnings("serial")
    static class SnakeException extends RuntimeException {
        public SnakeException() { super(); }
        public SnakeException(String msg) { super(msg); }
    }

    // Class-level opt-OUT: `@JsonNaming` with the "use default" pseudo-value wins
    // over a mapper-level strategy, so these keep their canonical names
    @JsonNaming(PropertyNamingStrategy.class)
    @SuppressWarnings("serial")
    static class OptOutException extends RuntimeException {
        public OptOutException() { super(); }
        public OptOutException(String msg) { super(msg); }
    }

    // Class-level opt-IN, with no mapper-level strategy configured
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @SuppressWarnings("serial")
    static class OptInException extends RuntimeException {
        public OptInException() { super(); }
        public OptInException(String msg) { super(msg); }
    }

    private final ObjectMapper SNAKE_MAPPER = jsonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    // "localized_message" is skipped (like "localizedMessage" is with default naming),
    // not reported as an unknown property
    @Test
    public void localizedMessageSkippedUnderSnakeCase() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        String json = """
                {"message":"the msg","localized_message":"the msg"}""";
        SnakeException ex = mapper.readValue(json, SnakeException.class);
        assertEquals("the msg", ex.getMessage());
    }

    // Control: same input with default naming has always worked
    @Test
    public void localizedMessageSkippedWithDefaultNaming() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        String json = """
                {"message":"the msg","localizedMessage":"the msg"}""";
        SnakeException ex = mapper.readValue(json, SnakeException.class);
        assertEquals("the msg", ex.getMessage());
    }

    // Null "stack_trace" must be skipped, not passed to `setStackTrace(null)` (NPE)
    @Test
    public void nullStackTraceSkippedUnderSnakeCase() throws Exception {
        String json = """
                {"message":"the msg","stack_trace":null,"cause":null}""";
        SnakeException ex = SNAKE_MAPPER.readValue(json, SnakeException.class);
        assertEquals("the msg", ex.getMessage());
        assertNull(ex.getCause());
    }

    // Full round-trip through the renamed names
    @Test
    public void roundTripUnderSnakeCase() throws Exception {
        SnakeException input = new SnakeException("the msg");
        input.initCause(new RuntimeException("root"));
        String json = SNAKE_MAPPER.writeValueAsString(input);
        assertTrue(json.contains("\"stack_trace\""), "expected renamed 'stack_trace' in: " + json);

        SnakeException ex = SNAKE_MAPPER.readValue(json, SnakeException.class);
        assertEquals("the msg", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("root", ex.getCause().getMessage());
    }

    // [databind#6188] Class-level `@JsonNaming` opting out must win over the
    // mapper-level strategy: names stay canonical, so "stackTrace" (not
    // "stack_trace") is the one that must be recognized
    @Test
    public void classLevelOptOutBeatsMapperStrategy() throws Exception {
        String json = """
                {"message":"the msg","stackTrace":null,"cause":null}""";
        OptOutException ex = SNAKE_MAPPER.readValue(json, OptOutException.class);
        assertEquals("the msg", ex.getMessage());
        assertNull(ex.getCause());
    }

    // ...and class-level `@JsonNaming` applies even with no mapper-level strategy
    @Test
    public void classLevelStrategyAppliesWithoutMapperStrategy() throws Exception {
        ObjectMapper plain = newJsonMapper();
        String json = """
                {"message":"the msg","stack_trace":null,"cause":null}""";
        OptInException ex = plain.readValue(json, OptInException.class);
        assertEquals("the msg", ex.getMessage());
        assertNull(ex.getCause());
    }
}

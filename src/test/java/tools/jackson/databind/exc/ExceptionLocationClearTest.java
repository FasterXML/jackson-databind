package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DeserializationFeature#EXCLUDE_LOCATION_IN_EXCEPTIONS}.
 *
 * @since 3.2
 */
public class ExceptionLocationClearTest extends DatabindTestUtil
{
    static class Point {
        public int x, y;
    }

    // By default, exception should include location info
    @Test
    public void testDefaultBehaviorHasLocation() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        try {
            mapper.readValue("{ broken }", Point.class);
            fail("Should not pass");
        } catch (JacksonException e) {
            assertNotNull(e.getLocation(), "Location should be present by default");
        }
    }

    // With feature enabled, location should be cleared
    @Test
    public void testExcludeLocationOnReadValue() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.EXCLUDE_LOCATION_IN_EXCEPTIONS)
                .build();
        try {
            mapper.readValue("{ broken }", Point.class);
            fail("Should not pass");
        } catch (JacksonException e) {
            assertNull(e.getLocation(),
                    "Location should be null when EXCLUDE_LOCATION_IN_EXCEPTIONS is enabled");
        }
    }

    // Via ObjectReader
    @Test
    public void testExcludeLocationViaObjectReader() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.EXCLUDE_LOCATION_IN_EXCEPTIONS)
                .build();
        ObjectReader reader = mapper.readerFor(Point.class);
        try {
            reader.readValue("{ broken }");
            fail("Should not pass");
        } catch (JacksonException e) {
            assertNull(e.getLocation(),
                    "Location should be null when EXCLUDE_LOCATION_IN_EXCEPTIONS is enabled via ObjectReader");
        }
    }

    // Also test readTree path
    @Test
    public void testExcludeLocationOnReadTree() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.EXCLUDE_LOCATION_IN_EXCEPTIONS)
                .build();
        try {
            mapper.readTree("{ broken }");
            fail("Should not pass");
        } catch (JacksonException e) {
            assertNull(e.getLocation(),
                    "Location should be null when EXCLUDE_LOCATION_IN_EXCEPTIONS is enabled for readTree");
        }
    }

    // Databind-level exception (not just streaming parse error)
    @Test
    public void testExcludeLocationOnDatabindException() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(DeserializationFeature.EXCLUDE_LOCATION_IN_EXCEPTIONS)
                .build();
        try {
            // Invalid type coercion should produce a DatabindException
            mapper.readValue("\"not a number\"", Point.class);
            fail("Should not pass");
        } catch (JacksonException e) {
            assertNull(e.getLocation(),
                    "Location should be null for databind-level exceptions too");
        }
    }
}

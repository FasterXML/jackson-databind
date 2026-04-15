package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5897]: fast path that skips TokenBuffer allocation for unknown
// properties must preserve observable behavior across the relevant toggles.
public class RecordUnknownProps5897Test extends DatabindTestUtil
{
    record Point(int x, int y) { }

    private static final String JSON_WITH_EXTRAS =
            "{\"x\":1,\"extra\":{\"nested\":[1,2,3]},\"y\":2,\"trailing\":\"ignored\"}";

    @Test
    public void testSkipUnknowns_defaultConfig() throws Exception {
        JsonMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        Point p = mapper.readValue(JSON_WITH_EXTRAS, Point.class);
        assertEquals(1, p.x());
        assertEquals(2, p.y());
    }

    @Test
    public void testFailOnUnknown_stillThrows() throws Exception {
        JsonMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue(JSON_WITH_EXTRAS, Point.class));
    }

    @Test
    public void testProblemHandler_stillInvoked() throws Exception {
        final java.util.List<String> seen = new java.util.ArrayList<>();
        JsonMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(
                            tools.jackson.databind.DeserializationContext ctxt,
                            tools.jackson.core.JsonParser p,
                            tools.jackson.databind.ValueDeserializer<?> deserializer,
                            Object beanOrClass, String propertyName) {
                        seen.add(propertyName);
                        try { p.skipChildren(); } catch (Exception e) { throw new RuntimeException(e); }
                        return true;
                    }
                })
                .build();
        Point p = mapper.readValue(JSON_WITH_EXTRAS, Point.class);
        assertEquals(1, p.x());
        assertEquals(2, p.y());
        assertTrue(seen.contains("extra"), "handler saw: " + seen);
        assertTrue(seen.contains("trailing"), "handler saw: " + seen);
    }
}

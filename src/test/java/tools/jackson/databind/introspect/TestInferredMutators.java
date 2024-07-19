package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestInferredMutators extends DatabindTestUtil
{
    public static class Point {
        protected int x;

        public int getX() { return x; }
    }

    public static class FixedPoint {
        protected final int x;

        public FixedPoint() { x = 0; }

        public int getX() { return x; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testFinalFieldIgnoral() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .build();
        try {
            /*p =*/ mapper.readValue("{\"x\":2}", FixedPoint.class);
            fail("Should not try to use final field");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property \"x\"");
        }
    }

    @Test
    public void testDeserializationInference() throws Exception
    {
        final String JSON = "{\"x\":2}";
        ObjectMapper mapper = jsonMapperBuilder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        // First: default case, inference enabled:
        assertTrue(mapper.isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS));
        Point p = mapper.readValue(JSON,  Point.class);
        assertEquals(2, p.x);

        // but without it, should fail:
        mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(MapperFeature.INFER_PROPERTY_MUTATORS)
                .build();
        try {
            p = mapper.readValue(JSON,  Point.class);
            fail("Should not succeeed");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized property \"x\"");
        }
    }
}

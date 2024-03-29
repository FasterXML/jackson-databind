package com.fasterxml.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

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

    // for #190
    @Test
    public void testFinalFieldIgnoral() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // default value is 'enabled', for backwards compatibility
        assertTrue(mapper.isEnabled(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS));
        mapper = jsonMapperBuilder()
                .disable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .build();
        try {
            /*p =*/ mapper.readValue("{\"x\":2}", FixedPoint.class);
            fail("Should not try to use final field");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "unrecognized field \"x\"");
        }
    }

    // for #195
    @Test
    public void testDeserializationInference() throws Exception
    {
        final String JSON = "{\"x\":2}";
        ObjectMapper mapper = new ObjectMapper();
        // First: default case, inference enabled:
        assertTrue(mapper.isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS));
        Point p = mapper.readValue(JSON,  Point.class);
        assertEquals(2, p.x);

        // but without it, should fail:
        mapper = jsonMapperBuilder()
                .disable(MapperFeature.INFER_PROPERTY_MUTATORS)
                .build();
        try {
            p = mapper.readValue(JSON,  Point.class);
            fail("Should not succeeed");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "unrecognized field \"x\"");
        }
    }
}

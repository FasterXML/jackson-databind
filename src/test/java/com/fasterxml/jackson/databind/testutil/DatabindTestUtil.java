package com.fasterxml.jackson.databind.testutil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class containing test utility methods.
 * The methods are migrated from {@link BaseMapTest} and {@link BaseTest},
 * as part of JUnit 5 migration.
 *
 * @since 2.17
 */
public class DatabindTestUtil
{
   /*
    /**********************************************************
    /* Shared helper classes
    /**********************************************************
     */

    public static class IntWrapper {
        public int i;

        public IntWrapper() { }
        public IntWrapper(int value) { i = value; }
    }

    public static class LongWrapper {
        public long l;

        public LongWrapper() { }
        public LongWrapper(long value) { l = value; }
    }

    public static class FloatWrapper {
        public float f;

        public FloatWrapper() { }
        public FloatWrapper(float value) { f = value; }
    }

    public static class DoubleWrapper {
        public double d;

        public DoubleWrapper() { }
        public DoubleWrapper(double value) { d = value; }
    }

    /**
     * Simple wrapper around String type, usually to test value
     * conversions or wrapping
     */
    public static class StringWrapper {
        public String str;

        public StringWrapper() { }
        public StringWrapper(String value) {
            str = value;
        }
    }

    public static enum ABC { A, B, C; }

    public static class Point {
        public int x, y;

        protected Point() { } // for deser
        public Point(int x0, int y0) {
            x = x0;
            y = y0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BaseMapTest.Point)) {
                return false;
            }
            BaseMapTest.Point other = (BaseMapTest.Point) o;
            return (other.x == x) && (other.y == y);
        }

        @Override
        public String toString() {
            return String.format("[x=%d, y=%d]", x, y);
        }
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    private static ObjectMapper SHARED_MAPPER;

    public static ObjectMapper sharedMapper() {
        if (SHARED_MAPPER == null) {
            SHARED_MAPPER = newJsonMapper();
        }
        return SHARED_MAPPER;
    }

    public static ObjectMapper objectMapper() {
        return sharedMapper();
    }

    public static ObjectWriter objectWriter() {
        return sharedMapper().writer();
    }

    public static ObjectReader objectReader() {
        return sharedMapper().reader();
    }

    public static ObjectReader objectReader(Class<?> cls) {
        return sharedMapper().readerFor(cls);
    }

    public static TypeFactory newTypeFactory() {
        // this is a work-around; no null modifier added
        return TypeFactory.defaultInstance().withModifier(null);
    }

    /*
    /**********************************************************
    /* Mapper construction helpers
    /**********************************************************
     */

    public static ObjectMapper newJsonMapper() {
        return new JsonMapper();
    }

    public static JsonMapper.Builder jsonMapperBuilder() {
        return JsonMapper.builder();
    }

    /*
    /**********************************************************
    /* Helper methods, serialization
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public static Map<String,Object> writeAndMap(ObjectMapper m, Object value)
            throws IOException
    {
        String str = m.writeValueAsString(value);
        return (Map<String,Object>) m.readValue(str, LinkedHashMap.class);
    }

    /*
    /**********************************************************
    /* Encoding or String representations
    /**********************************************************
     */

    public static String a2q(String json) {
        return json.replace("'", "\"");
    }

    public static String q(String str) {
        return '"'+str+'"';
    }

    public static byte[] utf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /*
    /**********************************************************
    /* Additional assertion methods
    /**********************************************************
     */

    public static void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    public static void assertToken(JsonToken expToken, JsonParser jp)
    {
        assertToken(expToken, jp.currentToken());
    }

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    public static void verifyException(Throwable e, String... anyMatches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : anyMatches) {
            String lmatch = match.toLowerCase();
            if (lmsg.contains(lmatch)) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("
            + Arrays.asList(anyMatches)+"): got one (of type "+e.getClass().getName()
            +") with message \""+msg+"\"");
    }

    public static void assertValidLocation(JsonLocation location) {
        assertNotNull(location, "Should have non-null location");
        assertTrue(location.getLineNr() > 0, "Should have positive line number");
    }
}

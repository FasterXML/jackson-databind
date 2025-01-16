package tools.jackson.databind.testutil;

import java.io.*;
import java.lang.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testutil.JacksonTestUtilBase;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class containing test utility methods.
 * The methods are migrated from {@code BaseMapTest} and {@code BaseTest},
 * as part of JUnit 5 migration.
 */
public class DatabindTestUtil
    extends JacksonTestUtilBase
{
    // @since 2.18
    // Helper annotation to work around lack of implicit name access with Jackson 2.x
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface ImplicitName {
        String value();
    }

    // @since 2.18
    @SuppressWarnings("serial")
    static public class ImplicitNameIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(MapperConfig<?> config,
                AnnotatedMember member) {
            final ImplicitName ann = member.getAnnotation(ImplicitName.class);
            return (ann == null) ? null : ann.value();
        }
    }

    private final static Object SINGLETON_OBJECT = new Object();

    private final static TypeFactory DEFAULT_TYPE_FACTORY = TypeFactory.createDefaultInstance();

    /*
    /**********************************************************************
    /* A sample documents
    /**********************************************************************
     */

    public static final int SAMPLE_SPEC_VALUE_WIDTH = 800;
    public static final int SAMPLE_SPEC_VALUE_HEIGHT = 600;
    public static final String SAMPLE_SPEC_VALUE_TITLE = "View from 15th Floor";
    public static final String SAMPLE_SPEC_VALUE_TN_URL = "http://www.example.com/image/481989943";
    public static final int SAMPLE_SPEC_VALUE_TN_HEIGHT = 125;
    public static final String SAMPLE_SPEC_VALUE_TN_WIDTH = "100";
    public static final int SAMPLE_SPEC_VALUE_TN_ID1 = 116;
    public static final int SAMPLE_SPEC_VALUE_TN_ID2 = 943;
    public static final int SAMPLE_SPEC_VALUE_TN_ID3 = 234;
    public static final int SAMPLE_SPEC_VALUE_TN_ID4 = 38793;

    public static final String SAMPLE_DOC_JSON_SPEC =
        "{\n"
            +"  \"Image\" : {\n"
            +"    \"Width\" : "+SAMPLE_SPEC_VALUE_WIDTH+",\n"
            +"    \"Height\" : "+SAMPLE_SPEC_VALUE_HEIGHT+","
            +"\"Title\" : \""+SAMPLE_SPEC_VALUE_TITLE+"\",\n"
            +"    \"Thumbnail\" : {\n"
            +"      \"Url\" : \""+SAMPLE_SPEC_VALUE_TN_URL+"\",\n"
            +"\"Height\" : "+SAMPLE_SPEC_VALUE_TN_HEIGHT+",\n"
            +"      \"Width\" : \""+SAMPLE_SPEC_VALUE_TN_WIDTH+"\"\n"
            +"    },\n"
            +"    \"IDs\" : ["+SAMPLE_SPEC_VALUE_TN_ID1+","+SAMPLE_SPEC_VALUE_TN_ID2+","+SAMPLE_SPEC_VALUE_TN_ID3+","+SAMPLE_SPEC_VALUE_TN_ID4+"]\n"
            +"  }"
            +"}"
        ;

    public static void verifyJsonSpecSampleDoc(JsonParser jp, boolean verifyContents)
    {
        verifyJsonSpecSampleDoc(jp, verifyContents, true);
    }

    public static void verifyJsonSpecSampleDoc(JsonParser jp, boolean verifyContents,
                                           boolean requireNumbers)
    {
        if (!jp.hasCurrentToken()) {
            jp.nextToken();
        }
        assertToken(JsonToken.START_OBJECT, jp.currentToken()); // main object

        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Image'
        if (verifyContents) {
            verifyFieldName(jp, "Image");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(jp, "Width");
        }

        verifyIntToken(jp.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(jp, "Height");
        }

        verifyIntToken(jp.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Title'
        if (verifyContents) {
            verifyFieldName(jp, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(jp));
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Thumbnail'
        if (verifyContents) {
            verifyFieldName(jp, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Url'
        if (verifyContents) {
            verifyFieldName(jp, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(jp));
        }
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(jp, "Height");
        }
        verifyIntToken(jp.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(jp, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(jp));
        }

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken()); // 'IDs'
        assertToken(JsonToken.START_ARRAY, jp.nextToken()); // 'ids' array
        verifyIntToken(jp.nextToken(), requireNumbers); // ids[0]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID1);
        }
        verifyIntToken(jp.nextToken(), requireNumbers); // ids[1]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID2);
        }
        verifyIntToken(jp.nextToken(), requireNumbers); // ids[2]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID3);
        }
        verifyIntToken(jp.nextToken(), requireNumbers); // ids[3]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID4);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken()); // 'ids' array

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // main object
    }

    private static void verifyIntToken(JsonToken t, boolean requireNumbers)
    {
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return;
        }
        if (requireNumbers) { // to get error
            assertToken(JsonToken.VALUE_NUMBER_INT, t);
        }
        // if not number, must be String
        if (t != JsonToken.VALUE_STRING) {
            fail("Expected INT or STRING value, got "+t);
        }
    }

    private static void verifyFieldName(JsonParser p, String expName)
    {
        assertEquals(expName, p.getString());
        assertEquals(expName, p.currentName());
    }

    private static void verifyIntValue(JsonParser p, long expValue)
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), p.getString());
    }

    /**
     * Enumeration type with sub-classes per value.
     */
    public enum EnumWithSubClass {
        A { @Override public void foobar() { } }
        ,B { @Override public void foobar() { } }
        ;

        public abstract void foobar();
    }

    /*
    /**********************************************************************
    /* Shared helper classes
    /**********************************************************************
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
            if (!(o instanceof Point)) {
                return false;
            }
            Point other = (Point) o;
            return (other.x == x) && (other.y == y);
        }

        @Override
        public String toString() {
            return String.format("[x=%d, y=%d]", x, y);
        }
    }

    public static class MapWrapper<K,V>
    {
        public Map<K,V> map;

        public MapWrapper() { }
        public MapWrapper(Map<K,V> m) {
            map = m;
        }
        public MapWrapper(K key, V value) {
            map = new LinkedHashMap<>();
            map.put(key, value);
        }
    }

    protected static class ListWrapper<T>
    {
        public List<T> list;

        public ListWrapper(@SuppressWarnings("unchecked") T... values) {
            list = new ArrayList<T>();
            for (T value : values) {
                list.add(value);
            }
        }
    }

    protected static class ArrayWrapper<T>
    {
        public T[] array;

        public ArrayWrapper(T[] v) {
            array = v;
        }
    }

    public static class BogusSchema implements FormatSchema {
        @Override
        public String getSchemaType() {
            return "TestFormat";
        }
    }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    private static ObjectMapper SHARED_MAPPER;

    public static ObjectMapper sharedMapper() {
        if (SHARED_MAPPER == null) {
            SHARED_MAPPER = newJsonMapper();
        }
        return SHARED_MAPPER;
    }

    protected ObjectMapper objectMapper() {
        return sharedMapper();
    }

    protected static ObjectWriter objectWriter() {
        return sharedMapper().writer();
    }

    protected static ObjectReader objectReader() {
        return sharedMapper().reader();
    }

    public static TypeFactory newTypeFactory() {
        // this is a work-around; no null modifier added
        return defaultTypeFactory().withModifier(null);
    }

    /*
    /**********************************************************************
    /* Mapper construction helpers
    /**********************************************************************
     */

    public static JsonMapper newJsonMapper() {
        return jsonMapperBuilder().build();
    }

    public static JsonMapper.Builder jsonMapperBuilder() {
        return JsonMapper.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
    }

    /*
    /**********************************************************************
    /* Helper methods, serialization
    /**********************************************************************
     */

    @SuppressWarnings("unchecked")
    public static Map<String,Object> writeAndMap(ObjectMapper m, Object value)
    {
        String str = m.writeValueAsString(value);
        return (Map<String,Object>) m.readValue(str, LinkedHashMap.class);
    }

    protected String serializeAsString(ObjectMapper m, Object value)
        throws IOException
    {
        return m.writeValueAsString(value);
    }

    protected String serializeAsString(Object value)
        throws IOException
    {
        return serializeAsString(sharedMapper(), value);
    }

    /*
    /**********************************************************************
    /* Additional assertion methods
    /**********************************************************************
     */

    public static void assertValidLocation(TokenStreamLocation location) {
        assertNotNull(location, "Should have non-null location");
        assertTrue(location.getLineNr() > 0, "Should have positive line number");
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    public static void verifyException(Exception e, Class<?> expType, String expMsg)
    {
        if (e.getClass() != expType) {
            fail("Expected exception of type "+expType.getName()+", got "+e.getClass().getName());
        }
        if (expMsg != null) {
            verifyException(e, expMsg);
        }
    }

    /**
     * Helper method for verifying 3 basic cookie cutter cases;
     * identity comparison (true), and against null (false),
     * or object of different type (false)
     */
    protected void assertStandardEquals(Object o)
    {
        assertTrue(o.equals(o));
        assertFalse(o.equals(null));
        assertFalse(o.equals(SINGLETON_OBJECT));
        // just for fun, let's also call hash code...
        o.hashCode();
    }

    /*
    /**********************************************************************
    /* Helper methods, other
    /**********************************************************************
     */

    public static TimeZone getUTCTimeZone() {
        return TimeZone.getTimeZone("GMT");
    }

    // Separated out since "default" TypeFactory instance handling differs
    // between 2.x and 3.x
    public static TypeFactory defaultTypeFactory() {
        return DEFAULT_TYPE_FACTORY;
    }

    protected JsonParser createParserUsingReader(String input)
        throws JacksonException
    {
        return createParserUsingReader(new JsonFactory(), input);
    }

    protected JsonParser createParserUsingReader(JsonFactory f, String input)
        throws JacksonException
    {
        return f.createParser(ObjectReadContext.empty(), new StringReader(input));
    }
}

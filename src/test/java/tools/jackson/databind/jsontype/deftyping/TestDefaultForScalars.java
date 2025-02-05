package tools.jackson.databind.jsontype.deftyping;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify that Java/JSON scalar values (non-structured values)
 * are handled properly with respect to additional type information.
 */
public class TestDefaultForScalars
    extends DatabindTestUtil
{
    static class Jackson417Bean {
        public String foo = "bar";
        public java.io.Serializable bar = Integer.valueOf(13);
    }

    // [databind#1395]: prevent attempts at including type info for primitives
    static class Data {
        public long key;
    }

    // Basic `ObjectWrapper` from base uses delegating ctor, won't work well; should
    // figure out why, but until then we'll use separate impl
    protected static class ObjectWrapperForPoly {
        Object object;

        protected ObjectWrapperForPoly() { }
        public ObjectWrapperForPoly(final Object o) {
            object = o;
        }
        public Object getObject() { return object; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper DEFAULT_TYPING_MAPPER = jsonMapperBuilder()
                    .activateDefaultTyping(NoCheckSubTypeValidator.instance)
                    .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .build();

    /**
     * Unit test to verify that limited number of core types do NOT include
     * type information, even if declared as Object. This is only done for types
     * that JSON scalar values natively map to: String, Integer and Boolean (and
     * nulls never have type information)
     */
    @Test
    public void testNumericScalars() throws Exception
    {
        // no typing for Integer, Double, yes for others
        assertEquals("[123]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Integer.valueOf(123) }));
        assertEquals("[[\"java.lang.Long\",37]]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Long.valueOf(37) }));
        assertEquals("[0.25]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Double.valueOf(0.25) }));
        assertEquals("[[\"java.lang.Float\",0.5]]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Float.valueOf(0.5f) }));
    }

    @Test
    public void testDateScalars() throws Exception
    {
        long ts = 12345678L;
        assertEquals("[[\"java.util.Date\","+ts+"]]",
                DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { new Date(ts) }));

        // Calendar is trickier... hmmh. Need to ensure round-tripping
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        String json = DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { c });
        assertEquals("[[\""+c.getClass().getName()+"\","+ts+"]]", json);
        // and let's make sure it also comes back same way:
        Object[] result = DEFAULT_TYPING_MAPPER.readValue(json, Object[].class);
        assertEquals(1, result.length);
        assertTrue(result[0] instanceof Calendar);
        assertEquals(ts, ((Calendar) result[0]).getTimeInMillis());
    }

    @Test
    public void testMiscScalars() throws Exception
    {
        // no typing for Strings, booleans
        assertEquals("[\"abc\"]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { "abc" }));
        assertEquals("[true,null,false]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Boolean[] { true, null, false }));
    }

    /**
     * Test for verifying that contents of "untyped" homogenous arrays are properly
     * handled,
     */
    @Test
    public void testScalarArrays() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.JAVA_LANG_OBJECT)
                .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        Object[] input = new Object[] {
                "abc", new Date(1234567), null, Integer.valueOf(456)
        };
        String json = mapper.writeValueAsString(input);
        assertEquals("[\"abc\",[\"java.util.Date\",1234567],null,456]", json);

        // and should deserialize back as well:
        Object[] output = mapper.readValue(json, Object[].class);
        assertArrayEquals(input, output);
    }

    // Loosely scalar
    @Test
    public void test417() throws Exception
    {
        Jackson417Bean input = new Jackson417Bean();
        String json = DEFAULT_TYPING_MAPPER.writeValueAsString(input);
        Jackson417Bean result = DEFAULT_TYPING_MAPPER.readValue(json, Jackson417Bean.class);
        assertEquals(input.foo, result.foo);
        assertEquals(input.bar, result.bar);
    }

    // [databind#1395]: prevent attempts at including type info for primitives
    @Test
    public void testDefaultTypingWithLong() throws Exception
    {
        Data data = new Data();
        data.key = 1L;
        Map<String, Object> mapData = new HashMap<String, Object>();
        mapData.put("longInMap", 2L);
        mapData.put("longAsField", data);

        // Configure Jackson to preserve types
        StdTypeResolverBuilder resolver = new StdTypeResolverBuilder(JsonTypeInfo.Id.CLASS,
                JsonTypeInfo.As.PROPERTY, "__t");
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .polymorphicTypeValidator(new NoCheckSubTypeValidator())
                .setDefaultTyping(resolver)
                .build();

        // Serialize
        String json = mapper.writeValueAsString(mapData);

        // Deserialize
        Map<?,?> result = mapper.readValue(json, Map.class);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // [databind#2236]: do need type info for NaN
    @Test
    public void testDefaultTypingWithNaN() throws Exception
    {
        final ObjectWrapperForPoly INPUT = new ObjectWrapperForPoly(Double.POSITIVE_INFINITY);
        final String json = DEFAULT_TYPING_MAPPER.writeValueAsString(INPUT);
        final ObjectWrapperForPoly result = DEFAULT_TYPING_MAPPER.readValue(json, ObjectWrapperForPoly.class);
        assertEquals(Double.class, result.getObject().getClass());
        assertEquals(INPUT.getObject().toString(), result.getObject().toString());
        assertTrue(((Double) result.getObject()).isInfinite());
    }
}

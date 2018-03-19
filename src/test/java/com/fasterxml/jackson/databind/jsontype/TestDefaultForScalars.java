package com.fasterxml.jackson.databind.jsontype;

import java.util.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests to verify that Java/JSON scalar values (non-structured values)
 * are handled properly with respect to additional type information.
 */
public class TestDefaultForScalars
    extends BaseMapTest
{
    static class Jackson417Bean {
        public String foo = "bar";
        public java.io.Serializable bar = new Integer(13);
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper DEFAULT_TYPING_MAPPER = ObjectMapper.builder()
                    .enableDefaultTyping()
                    .build();

    /**
     * Unit test to verify that limited number of core types do NOT include
     * type information, even if declared as Object. This is only done for types
     * that JSON scalar values natively map to: String, Integer and Boolean (and
     * nulls never have type information)
     */
    public void testNumericScalars() throws Exception
    {
        // no typing for Integer, Double, yes for others
        assertEquals("[123]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Integer.valueOf(123) }));
        assertEquals("[[\"java.lang.Long\",37]]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Long.valueOf(37) }));
        assertEquals("[0.25]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Double.valueOf(0.25) }));
        assertEquals("[[\"java.lang.Float\",0.5]]", DEFAULT_TYPING_MAPPER.writeValueAsString(new Object[] { Float.valueOf(0.5f) }));
    }

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
    public void testScalarArrays() throws Exception
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .enableDefaultTyping(DefaultTyping.JAVA_LANG_OBJECT)
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
    public void test417() throws Exception
    {
        Jackson417Bean input = new Jackson417Bean();
        String json = DEFAULT_TYPING_MAPPER.writeValueAsString(input);
        Jackson417Bean result = DEFAULT_TYPING_MAPPER.readValue(json, Jackson417Bean.class);
        assertEquals(input.foo, result.foo);
        assertEquals(input.bar, result.bar);
    }
}

package com.fasterxml.jackson.databind.ser;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking handling of some of {@link MapperFeature}s
 * and {@link SerializationFeature}s for serialization.
 */
public class SerializationFeaturesTest
    extends BaseMapTest
{
    static class CloseableBean implements Closeable
    {
        public int a = 3;

        protected boolean wasClosed = false;

        @Override
        public void close() throws IOException {
            wasClosed = true;
        }
    }

    private static class StringListBean {
        @SuppressWarnings("unused")
        public Collection<String> values;

        public StringListBean(Collection<String> v) { values = v; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // Test for [JACKSON-282]
    @SuppressWarnings("resource")
    public void testCloseCloseable() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        // default should be disabled:
        CloseableBean bean = new CloseableBean();
        m.writeValueAsString(bean);
        assertFalse(bean.wasClosed);

        // but can enable it:
        m.configure(SerializationFeature.CLOSE_CLOSEABLE, true);
        bean = new CloseableBean();
        m.writeValueAsString(bean);
        assertTrue(bean.wasClosed);

        // also: let's ensure that ObjectWriter won't interfere with it
        bean = new CloseableBean();
        m.writerFor(CloseableBean.class).writeValueAsString(bean);
        assertTrue(bean.wasClosed);
    }

    // Test for [JACKSON-289]
    public void testCharArrays() throws IOException
    {
        char[] chars = new char[] { 'a','b','c' };
        ObjectMapper m = new ObjectMapper();
        // default: serialize as Strings
        assertEquals(q("abc"), m.writeValueAsString(chars));

        // new feature: serialize as JSON array:
        m.configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true);
        assertEquals("[\"a\",\"b\",\"c\"]", m.writeValueAsString(chars));
    }

    // Test for [JACKSON-401]
    public void testFlushingAutomatic() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(mapper.getSerializationConfig().isEnabled(SerializationFeature.FLUSH_AFTER_WRITE_VALUE));
        // default is to flush after writeValue()
        StringWriter sw = new StringWriter();
        JsonGenerator g = mapper.createGenerator(sw);
        mapper.writeValue(g, Integer.valueOf(13));
        assertEquals("13", sw.toString());
        g.close();

        // ditto with ObjectWriter
        sw = new StringWriter();
        g = mapper.createGenerator(sw);
        ObjectWriter ow = mapper.writer();
        ow.writeValue(g, Integer.valueOf(99));
        assertEquals("99", sw.toString());
        g.close();
    }

    public void testFlushingNotAutomatic() throws IOException
    {
        // but should not occur if configured otherwise
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false);
        StringWriter sw = new StringWriter();
        JsonGenerator g = mapper.createGenerator(sw);

        mapper.writeValue(g, Integer.valueOf(13));
        // no flushing now:
        assertEquals("", sw.toString());
        // except when actually flushing
        g.flush();
        assertEquals("13", sw.toString());
        g.close();
        // Also, same should happen with ObjectWriter
        sw = new StringWriter();
        g = mapper.createGenerator(sw);
        ObjectWriter ow = mapper.writer();
        ow.writeValue(g, Integer.valueOf(99));
        assertEquals("", sw.toString());
        // except when actually flushing
        g.flush();
        assertEquals("99", sw.toString());
        g.close();
    }

    public void testSingleElementCollections() throws IOException
    {
        final ObjectWriter writer = objectWriter().with(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);

        // Lists:
        ArrayList<String> strs = new ArrayList<String>();
        strs.add("xyz");
        assertEquals(q("xyz"), writer.writeValueAsString(strs));
        ArrayList<Integer> ints = new ArrayList<Integer>();
        ints.add(13);
        assertEquals("13", writer.writeValueAsString(ints));

        // other Collections, like Sets:
        HashSet<Long> longs = new HashSet<Long>();
        longs.add(42L);
        assertEquals("42", writer.writeValueAsString(longs));
        // [databind#180]
        final String EXP_STRINGS = "{\"values\":\"foo\"}";
        assertEquals(EXP_STRINGS, writer.writeValueAsString(new StringListBean(Collections.singletonList("foo"))));

        final Set<String> SET = new HashSet<String>();
        SET.add("foo");
        assertEquals(EXP_STRINGS, writer.writeValueAsString(new StringListBean(SET)));

        // arrays:
        assertEquals("true", writer.writeValueAsString(new boolean[] { true }));
        assertEquals("[true,false]", writer.writeValueAsString(new boolean[] { true, false }));
        assertEquals("true", writer.writeValueAsString(new Boolean[] { Boolean.TRUE }));

        assertEquals("3", writer.writeValueAsString(new short[] { 3 }));
        assertEquals("[3,2]", writer.writeValueAsString(new short[] { 3, 2 }));

        assertEquals("3", writer.writeValueAsString(new int[] { 3 }));
        assertEquals("[3,2]", writer.writeValueAsString(new int[] { 3, 2 }));

        assertEquals("1", writer.writeValueAsString(new long[] { 1L }));
        assertEquals("[-1,4]", writer.writeValueAsString(new long[] { -1L, 4L }));

        assertEquals("0.5", writer.writeValueAsString(new double[] { 0.5 }));
        assertEquals("[0.5,2.5]", writer.writeValueAsString(new double[] { 0.5, 2.5 }));

        assertEquals("0.5", writer.writeValueAsString(new float[] { 0.5f }));
        assertEquals("[0.5,2.5]", writer.writeValueAsString(new float[] { 0.5f, 2.5f }));

        assertEquals(q("foo"), writer.writeValueAsString(new String[] { "foo" }));
    }
}

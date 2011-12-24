package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertArrayEquals;

public class TestUpdateValue extends BaseMapTest
{
    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    static class Bean {
        public String a = "a";
        public String b = "b";

        public int[] c = new int[] { 1, 2, 3 };

        public Bean child = null;
    }

    static class XYBean {
        public int x, y;
    }
    
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */
    
    public void testBeanUpdate() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Bean bean = new Bean();
        assertEquals("b", bean.b);
        assertEquals(3, bean.c.length);
        assertNull(bean.child);

        Object ob = m.readerForUpdating(bean).readValue("{ \"b\":\"x\", \"c\":[4,5], \"child\":{ \"a\":\"y\"} }");
        assertSame(ob, bean);

        assertEquals("a", bean.a);
        assertEquals("x", bean.b);
        assertArrayEquals(new int[] { 4, 5 }, bean.c);

        Bean child = bean.child;
        assertNotNull(child);
        assertEquals("y", child.a);
        assertEquals("b", child.b);
        assertArrayEquals(new int[] { 1, 2, 3 }, child.c);
        assertNull(child.child);
    }

    public void testListUpdate() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        List<String> strs = new ArrayList<String>();
        strs.add("a");
        // for lists, we will be appending entries
        Object ob = m.readerForUpdating(strs).readValue("[ \"b\", \"c\", \"d\" ]");
        assertSame(strs, ob);
        assertEquals(4, strs.size());
        assertEquals("a", strs.get(0));
        assertEquals("b", strs.get(1));
        assertEquals("c", strs.get(2));
        assertEquals("d", strs.get(3));
    }

    public void testMapUpdate() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,String> strs = new HashMap<String,String>();
        strs.put("a", "a");
        strs.put("b", "b");
        // for maps, we will be adding and/or overwriting entries
        Object ob = m.readerForUpdating(strs).readValue("{ \"c\" : \"c\", \"a\" : \"z\" }");
        assertSame(strs, ob);
        assertEquals(3, strs.size());
        assertEquals("z", strs.get("a"));
        assertEquals("b", strs.get("b"));
        assertEquals("c", strs.get("c"));
    }

    // Test for [JACKSON-717] -- ensure 'readValues' also does update
    public void testUpdateSequence() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        XYBean toUpdate = new XYBean();
        Iterator<XYBean> it = m.readerForUpdating(toUpdate).readValues(
                "{\"x\":1,\"y\":2}\n{\"x\":16}{\"y\":37}");

        assertTrue(it.hasNext());
        XYBean value = it.next();
        assertSame(toUpdate, value);
        assertEquals(1, value.x);
        assertEquals(2, value.y);

        assertTrue(it.hasNext());
        value = it.next();
        assertSame(toUpdate, value);
        assertEquals(16, value.x);
        assertEquals(2, value.y); // unchanged

        assertTrue(it.hasNext());
        value = it.next();
        assertSame(toUpdate, value);
        assertEquals(16, value.x); // unchanged
        assertEquals(37, value.y);
        
        assertFalse(it.hasNext());
    }
}

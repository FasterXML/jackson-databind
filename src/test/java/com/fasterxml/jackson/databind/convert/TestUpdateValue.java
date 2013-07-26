package com.fasterxml.jackson.databind.convert;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertArrayEquals;

/**
 * Unit tests for verifying that "updating reader" works as
 * expected.
 */
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

    // [JACKSON-824]
    public class TextView {}
    public class NumView {}

    public class Updateable {
        @JsonView(NumView.class)
        public int num;

        @JsonView(TextView.class)
        public String str;
    }
        
    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testBeanUpdate() throws Exception
    {
        Bean bean = new Bean();
        assertEquals("b", bean.b);
        assertEquals(3, bean.c.length);
        assertNull(bean.child);

        Object ob = MAPPER.readerForUpdating(bean).readValue("{ \"b\":\"x\", \"c\":[4,5], \"child\":{ \"a\":\"y\"} }");
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
        List<String> strs = new ArrayList<String>();
        strs.add("a");
        // for lists, we will be appending entries
        Object ob = MAPPER.readerForUpdating(strs).readValue("[ \"b\", \"c\", \"d\" ]");
        assertSame(strs, ob);
        assertEquals(4, strs.size());
        assertEquals("a", strs.get(0));
        assertEquals("b", strs.get(1));
        assertEquals("c", strs.get(2));
        assertEquals("d", strs.get(3));
    }

    public void testMapUpdate() throws Exception
    {
        Map<String,String> strs = new HashMap<String,String>();
        strs.put("a", "a");
        strs.put("b", "b");
        // for maps, we will be adding and/or overwriting entries
        Object ob = MAPPER.readerForUpdating(strs).readValue("{ \"c\" : \"c\", \"a\" : \"z\" }");
        assertSame(strs, ob);
        assertEquals(3, strs.size());
        assertEquals("z", strs.get("a"));
        assertEquals("b", strs.get("b"));
        assertEquals("c", strs.get("c"));
    }

    // Test for [JACKSON-717] -- ensure 'readValues' also does update
    @SuppressWarnings("resource")
    public void testUpdateSequence() throws Exception
    {
        XYBean toUpdate = new XYBean();
        Iterator<XYBean> it = MAPPER.readerForUpdating(toUpdate).readValues(
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

    // [JACKSON-824]
    public void testUpdatingWithViews() throws Exception
    {
        Updateable bean = new Updateable();
        bean.num = 100;
        bean.str = "test";
        Updateable result = MAPPER.readerForUpdating(bean)
                .withView(TextView.class)
                .readValue("{\"num\": 10, \"str\":\"foobar\"}");    
        assertSame(bean, result);

        assertEquals(100, bean.num);
        assertEquals("foobar", bean.str);
    }
}

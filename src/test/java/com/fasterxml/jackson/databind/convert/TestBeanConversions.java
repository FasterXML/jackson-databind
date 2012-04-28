package com.fasterxml.jackson.databind.convert;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestBeanConversions
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    final ObjectMapper MAPPER = new ObjectMapper();

    static class Point {
        public int x, y;

        public int z = -13;

        public Point() { }
        public Point(int a, int b, int c)
        {
            x = a;
            y = b;
            z = c;
        }
    }

    static class PointStrings {
        public final String x, y;

        public PointStrings(String x, String y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class BooleanBean {
        public boolean boolProp;
    }

    static class WrapperBean {
        public BooleanBean x;
    }

    static class ObjectWrapper
    {
        private Object data;

        public ObjectWrapper() { }
        public ObjectWrapper(Object o) { data = o; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testBeanConvert()
    {
        // should have no problems convert between compatible beans...
        PointStrings input = new PointStrings("37", "-9");
        Point point = MAPPER.convertValue(input, Point.class);
        assertEquals(37, point.x);
        assertEquals(-9, point.y);
        // z not included in input, will be whatever default constructor provides
        assertEquals(-13, point.z);
    }
    
    // For [JACKSON-371]; verify that we know property that caused issue...
    // (note: not optimal place for test, but will have to do for now)
    public void testErrorReporting() throws Exception
    {
        //String json = "{\"boolProp\":\"oops\"}";
        // First: unknown property
        try {
            MAPPER.readValue("{\"unknownProp\":true}", BooleanBean.class);
        } catch (JsonProcessingException e) {
            verifyException(e, "unknownProp");
        }

        // then bad conversion
        try {
            MAPPER.readValue("{\"boolProp\":\"foobar\"}", BooleanBean.class);
        } catch (JsonProcessingException e) {
            verifyException(e, "boolProp");
        }
    }

    public void testIssue458() throws Exception
    {
        ObjectWrapper a = new ObjectWrapper("foo");
        ObjectWrapper b = new ObjectWrapper(a);
        ObjectWrapper b2 = MAPPER.convertValue(b, ObjectWrapper.class);
        ObjectWrapper a2 = MAPPER.convertValue(b2.getData(), ObjectWrapper.class);
        assertEquals("foo", a2.getData());
    }

    // [JACKSON-710]: should work regardless of wrapping...
    public void testWrapping() throws Exception
    {
        ObjectMapper wrappingMapper = new ObjectMapper();
        wrappingMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        wrappingMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);

        // conversion is ok, even if it's bogus one
        _convertAndVerifyPoint(wrappingMapper);

        // also: ok to have mismatched settings, since as per [JACKSON-710], should
        // not actually use wrapping internally in these cases
        wrappingMapper = new ObjectMapper();
        wrappingMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        wrappingMapper.disable(SerializationFeature.WRAP_ROOT_VALUE);
        _convertAndVerifyPoint(wrappingMapper);

        wrappingMapper = new ObjectMapper();
        wrappingMapper.disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        wrappingMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        _convertAndVerifyPoint(wrappingMapper);
    }

    // [Issue-11]: simple cast, for POJOs etc
    public void testConvertUsingCast() throws Exception
    {
        String str = new String("foo");
        CharSequence seq = str;
        String result = MAPPER.convertValue(seq, String.class);
        // should just cast...
        assertSame(str, result);
    }
    
    // [Issue-11]: simple cast, for Tree
    public void testNodeConvert() throws Exception
    {
        ObjectNode src = (ObjectNode) MAPPER.readTree("{}");
        TreeNode node = src;
        ObjectNode result = MAPPER.treeToValue(node, ObjectNode.class);
        // should just cast...
        assertSame(src, result);
    }
    
    private void _convertAndVerifyPoint(ObjectMapper m)
    {
        final Point input = new Point(1, 2, 3);
        Point output = m.convertValue(input, Point.class);
        assertEquals(1, output.x);
        assertEquals(2, output.y);
        assertEquals(3, output.z);
    }
}

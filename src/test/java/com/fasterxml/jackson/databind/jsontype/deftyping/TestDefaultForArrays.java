package com.fasterxml.jackson.databind.jsontype.deftyping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestDefaultForArrays extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class ArrayBean {
        public Object[] values;

        public ArrayBean() { this(null); }
        public ArrayBean(Object[] v) { values = v; }
    }

    static class PrimitiveArrayBean {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        public Object stuff;

        protected PrimitiveArrayBean() { }
        public PrimitiveArrayBean(Object value) { stuff = value; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * Simple unit test for verifying that we get String array
     * back, even though declared type is Object array
     */
    public void testArrayTypingSimple() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder().
                activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_CONCRETE_AND_ARRAYS)
                .build();
        ArrayBean bean = new ArrayBean(new String[0]);
        String json = m.writeValueAsString(bean);
        ArrayBean result = m.readValue(json, ArrayBean.class);
        assertNotNull(result.values);
        assertEquals(String[].class, result.values.getClass());
    }

    // And let's try it with deeper array as well
    public void testArrayTypingNested() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_CONCRETE_AND_ARRAYS)
                .build();
        ArrayBean bean = new ArrayBean(new String[0][0]);
        String json = m.writeValueAsString(bean);
        ArrayBean result = m.readValue(json, ArrayBean.class);
        assertNotNull(result.values);
        assertEquals(String[][].class, result.values.getClass());
    }

    public void testNodeInArray() throws Exception
    {
        JsonNode node = new ObjectMapper().readTree("{\"a\":3}");
        ObjectMapper m = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.JAVA_LANG_OBJECT)
                .build();
        Object[] obs = new Object[] { node };
        String json = m.writeValueAsString(obs);
        Object[] result = m.readValue(json, Object[].class);
        assertEquals(1, result.length);
        Object ob = result[0];
        assertTrue(ob instanceof JsonNode);
    }

    @SuppressWarnings("deprecation")
    public void testNodeInEmptyArray() throws Exception {
        Map<String, List<String>> outerMap = new HashMap<String, List<String>>();
        outerMap.put("inner", new ArrayList<String>());
        ObjectMapper m = new ObjectMapper().disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        JsonNode tree = m.convertValue(outerMap, JsonNode.class);

        String json = m.writeValueAsString(tree);
        assertEquals("{}", json);

        JsonNode node = new ObjectMapper().readTree("{\"a\":[]}");

        m = jsonMapperBuilder()
                .disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.JAVA_LANG_OBJECT)
                .build();
        m.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                DefaultTyping.JAVA_LANG_OBJECT);

        Object[] obs = new Object[] { node };
        json = m.writeValueAsString(obs);
        Object[] result = m.readValue(json, Object[].class);

        assertEquals(1, result.length);
        Object elem = result[0];
        assertTrue(elem instanceof ObjectNode);
        assertEquals(0, ((ObjectNode) elem).size());
    }

    public void testArraysOfArrays() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();

        Object value = new Object[][] { new Object[] {} };
        String json = mapper.writeValueAsString(value);

        // try with different (but valid) nominal types:
        _testArraysAs(mapper, json, Object[][].class);
        _testArraysAs(mapper, json, Object[].class);
        _testArraysAs(mapper, json, Object.class);
    }

    public void testArrayTypingForPrimitiveArrays() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_CONCRETE_AND_ARRAYS);
        _testArrayTypingForPrimitiveArrays(m, new int[] { 1, 2, 3 });
        _testArrayTypingForPrimitiveArrays(m, new long[] { 1, 2, 3 });
        _testArrayTypingForPrimitiveArrays(m, new short[] { 1, 2, 3 });
        _testArrayTypingForPrimitiveArrays(m, new double[] { 0.5, 5.5, -1.0 });
        _testArrayTypingForPrimitiveArrays(m, new float[] { 0.5f, 5.5f, -1.0f });
        _testArrayTypingForPrimitiveArrays(m, new boolean[] { true, false });
        _testArrayTypingForPrimitiveArrays(m, new byte[] { 1, 2, 3 });

        _testArrayTypingForPrimitiveArrays(m, new char[] { 'a', 'b' });
    }

    private void _testArrayTypingForPrimitiveArrays(ObjectMapper mapper, Object v) throws Exception {
        PrimitiveArrayBean input = new PrimitiveArrayBean(v);
        String json = mapper.writeValueAsString(input);
        PrimitiveArrayBean result = mapper.readValue(json, PrimitiveArrayBean.class);
        assertNotNull(result.stuff);
        assertSame(v.getClass(), result.stuff.getClass());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected void _testArraysAs(ObjectMapper mapper, String json, Class<?> type)
        throws Exception
    {
        Object o = mapper.readValue(json, type);
        assertNotNull(o);
        assertTrue(o instanceof Object[]);
        Object[] main = (Object[]) o;
        assertEquals(1, main.length);
        Object element = main[0];
        assertNotNull(element);
        assertTrue(element instanceof Object[]);
        assertEquals(0, ((Object[]) element).length);
    }
}

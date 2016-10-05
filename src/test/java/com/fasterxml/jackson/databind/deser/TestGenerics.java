package com.fasterxml.jackson.databind.deser;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class TestGenerics
    extends BaseMapTest
{
    static abstract class BaseNumberBean<T extends Number>
    {
        public abstract void setNumber(T value);
    }

    static class NumberBean
        extends BaseNumberBean<Long>
    {
        long _number;

        @Override
        public void setNumber(Long value)
        {
            _number = value.intValue();
        }
    }

    /**
     * Very simple bean class
     */
    static class SimpleBean
    {
        public int x;
    }

    static class Wrapper<T>
    {
        public T value;

        public Wrapper() { }

        public Wrapper(T v) { value = v; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper<?>) && (((Wrapper<?>) o).value.equals(value));
        }
    }

    /*
    /***************************************************
    /* Test cases
    /***************************************************
     */

    public void testSimpleNumberBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        NumberBean result = mapper.readValue("{\"number\":17}", NumberBean.class);
        assertEquals(17, result._number);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    public void testGenericWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper<SimpleBean> result = mapper.readValue
            ("{\"value\": { \"x\" : 13 } }",
             new TypeReference<Wrapper<SimpleBean>>() { });
        assertNotNull(result);
        assertEquals(Wrapper.class, result.getClass());
        Object contents = result.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(13, bean.x);
    }

    public void testGenericWrapperWithSingleElementArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        Wrapper<SimpleBean> result = mapper.readValue
            ("[{\"value\": [{ \"x\" : 13 }] }]",
             new TypeReference<Wrapper<SimpleBean>>() { });
        assertNotNull(result);
        assertEquals(Wrapper.class, result.getClass());
        Object contents = result.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(13, bean.x);
    }

    /**
     * Unit test for verifying that we can use different
     * type bindings for individual generic types;
     * problem with [JACKSON-190]
     */
    public void testMultipleWrappers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // First, numeric wrapper
        Wrapper<Boolean> result = mapper.readValue
            ("{\"value\": true}", new TypeReference<Wrapper<Boolean>>() { });
        assertEquals(new Wrapper<>(Boolean.TRUE), result);

        // Then string one
        Wrapper<String> result2 = mapper.readValue
            ("{\"value\": \"abc\"}", new TypeReference<Wrapper<String>>() { });
        assertEquals(new Wrapper<>("abc"), result2);

        // And then number
        Wrapper<Long> result3 = mapper.readValue
            ("{\"value\": 7}", new TypeReference<Wrapper<Long>>() { });
        assertEquals(new Wrapper<>(7L), result3);
    }

    //[Issue#381]
    public void testMultipleWrappersSingleValueArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        // First, numeric wrapper
        Wrapper<Boolean> result = mapper.readValue
            ("[{\"value\": [true]}]", new TypeReference<Wrapper<Boolean>>() { });
        assertEquals(new Wrapper<>(Boolean.TRUE), result);

        // Then string one
        Wrapper<String> result2 = mapper.readValue
            ("[{\"value\": [\"abc\"]}]", new TypeReference<Wrapper<String>>() { });
        assertEquals(new Wrapper<>("abc"), result2);

        // And then number
        Wrapper<Long> result3 = mapper.readValue
            ("[{\"value\": [7]}]", new TypeReference<Wrapper<Long>>() { });
        assertEquals(new Wrapper<>(7L), result3);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    public void testArrayOfGenericWrappers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper<SimpleBean>[] result = mapper.readValue
            ("[ {\"value\": { \"x\" : 9 } } ]",
             new TypeReference<Wrapper<SimpleBean>[]>() { });
        assertNotNull(result);
        assertEquals(Wrapper[].class, result.getClass());
        assertEquals(1, result.length);
        Wrapper<SimpleBean> elem = result[0];
        Object contents = elem.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(9, bean.x);
    }

    // [Issue#381]
    public void testArrayOfGenericWrappersSingleValueArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS);

        Wrapper<SimpleBean>[] result = mapper.readValue
            ("[ {\"value\": [ { \"x\" : [ 9 ] } ] } ]",
             new TypeReference<Wrapper<SimpleBean>[]>() { });
        assertNotNull(result);
        assertEquals(Wrapper[].class, result.getClass());
        assertEquals(1, result.length);
        Wrapper<SimpleBean> elem = result[0];
        Object contents = elem.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(9, bean.x);
    }
}

package com.fasterxml.jackson.databind.jsontype.deftyping;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestDefaultForLists
    extends BaseMapTest
{
    /**
     * Wrapper bean needed before there is a way to force
     * type of the root value. Long is used because it is a final
     * type, but not one of "untypeable" ones.
     */
    static class ListOfLongs {
        public List<Long> longs;

        public ListOfLongs() { }
        public ListOfLongs(Long ... ls) {
            longs = new ArrayList<Long>();
            for (Long l: ls) {
                longs.add(l);
            }
        }
    }

    static class ListOfNumbers {
        public List<Number> nums;

        public ListOfNumbers() { }
        public ListOfNumbers(Number ... numbers) {
            nums = new ArrayList<Number>();
            for (Number n : numbers) {
                nums.add(n);
            }
        }
    }

    static class ObjectListBean {
        public List<Object> values;
    }

    interface Foo { }

    static class SetBean {
        public Set<String> names;

        public SetBean() { }
        public SetBean(String str) {
            names = new HashSet<String>();
            names.add(str);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper POLY_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance)
            .build();

    public void testListOfLongs() throws Exception
    {
        ListOfLongs input = new ListOfLongs(1L, 2L, 3L);
        String json = POLY_MAPPER.writeValueAsString(input);
        assertEquals("{\"longs\":[\"java.util.ArrayList\",[1,2,3]]}", json);
        ListOfLongs output = POLY_MAPPER.readValue(json, ListOfLongs.class);

        assertNotNull(output.longs);
        assertEquals(3, output.longs.size());
        assertEquals(Long.valueOf(1L), output.longs.get(0));
        assertEquals(Long.valueOf(2L), output.longs.get(1));
        assertEquals(Long.valueOf(3L), output.longs.get(2));
    }

    /**
     * Then bit more heterogenous list; also tests mixing of
     * regular scalar types, and non-typed ones (int and double
     * will never have type info added; other numbers will if
     * necessary)
     */
    public void testListOfNumbers() throws Exception
    {
        ListOfNumbers input = new ListOfNumbers(Long.valueOf(1L), Integer.valueOf(2), Double.valueOf(3.0));
        String json = POLY_MAPPER.writeValueAsString(input);
        assertEquals("{\"nums\":[\"java.util.ArrayList\",[[\"java.lang.Long\",1],2,3.0]]}", json);
        ListOfNumbers output = POLY_MAPPER.readValue(json, ListOfNumbers.class);

        assertNotNull(output.nums);
        assertEquals(3, output.nums.size());
        assertEquals(Long.valueOf(1L), output.nums.get(0));
        assertEquals(Integer.valueOf(2), output.nums.get(1));
        assertEquals(Double.valueOf(3.0), output.nums.get(2));
    }

    public void testDateTypes() throws Exception
    {
        ObjectListBean input = new ObjectListBean();
        List<Object> inputList = new ArrayList<Object>();
        inputList.add(TimeZone.getTimeZone("EST"));
        inputList.add(Locale.CHINESE);
        input.values = inputList;
        String json = POLY_MAPPER.writeValueAsString(input);

        ObjectListBean output = POLY_MAPPER.readValue(json, ObjectListBean.class);
        List<Object> outputList = output.values;
        assertEquals(2, outputList.size());
        assertTrue(outputList.get(0) instanceof TimeZone);
        assertTrue(outputList.get(1) instanceof Locale);
    }

    public void testJackson628() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL);
        ArrayList<Foo> data = new ArrayList<Foo>();
        String json = mapper.writeValueAsString(data);
        List<?> output = mapper.readValue(json, List.class);
        assertTrue(output.isEmpty());
    }

    public void testJackson667() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(NoCheckSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        String json = mapper.writeValueAsString(new SetBean("abc"));
        SetBean bean = mapper.readValue(json, SetBean.class);
        assertNotNull(bean);
        assertTrue(bean.names instanceof HashSet);
    }
}

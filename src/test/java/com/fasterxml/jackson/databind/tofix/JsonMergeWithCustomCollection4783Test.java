package com.fasterxml.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#4783] Test to verify that JsonMerge also works for custom list
public class JsonMergeWithCustomCollection4783Test
{

    private static class MyArrayListJDK<T> extends ArrayList<T> implements List<T> { }

    static class MergeListJDK {
        @JsonMerge
        @JsonProperty
        public List<String> values = create();
        { values.add("a");}
    }

    static <T> List<T> create() {
        return new MyArrayListJDK<T>();
    }

    interface MyListCustom<T> extends List<T> { }

    private static class MyArrayListCustom<T> extends ArrayList<T> implements MyListCustom<T> { }

    static class MergeCustomStringList {
        @JsonMerge
        @JsonProperty
        public MyListCustom<String> values = createCustomStringList();
        { values.add("a"); }
    }

    static <T> MyListCustom<T> createCustomStringList() {
        return new MyArrayListCustom<T>();
    }

    interface MyCustomLongList<T> extends List<T> { }

    private static class MyArrayCustomLongList<T> extends ArrayList<T> implements MyCustomLongList<T> { }

    static class MergeMyCustomLongList {
        @JsonMerge
        @JsonProperty
        public MyCustomLongList<String> values = createCustomLongList();
        { values.add("a"); }
    }

    static <T> MyCustomLongList<T> createCustomLongList() {
        return new MyArrayCustomLongList<T>();
    }

    interface MyCustomPojoList<T> extends List<T> { }

    private static class MyArrayCustomPojoList<T> extends ArrayList<T> implements MyCustomPojoList<T> { }

    static class MergeMyCustomPojoList {
        @JsonMerge
        @JsonProperty
        public MyCustomPojoList<CustomPojo> values = createCustomPojoList();
    }

    private static MyCustomPojoList<CustomPojo> createCustomPojoList() {
        MyCustomPojoList list = new MyArrayCustomPojoList<CustomPojo>();
        list.add(CustomPojo.create("a", 1));
        list.add(CustomPojo.create("b", 2));
        return list;
    }

    public static class CustomPojo {
        public String name;
        public int age;

        public static CustomPojo create(String name, int age) {
            CustomPojo pojo = new CustomPojo();
            pojo.name = name;
            pojo.age = age;
            return pojo;
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void testJDKMapperReading() throws Exception {
        MergeListJDK result = MAPPER.readValue("{\"values\":[\"x\"]}", MergeListJDK.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
    }

    @Test
    void testCustomMapperReading() throws Exception {
        MergeCustomStringList result = MAPPER.readValue("{\"values\":[\"x\"]}", MergeCustomStringList.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
    }

    @Test
    void testCustomMapperReadingLongArrayList() throws Exception {
        MergeMyCustomLongList result = MAPPER.readValue("{\"values\":[\"x\"]}", MergeMyCustomLongList.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
    }

    @Test
    void testCustomMapperReadingPojoArrayList() throws Exception {
        MergeMyCustomPojoList result = MAPPER.readValue("{\"values\":[{\"name\":\"c\",\"age\":3}]}", MergeMyCustomPojoList.class);

        assertEquals(3, result.values.size());
    }

}

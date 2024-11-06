package com.fasterxml.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.failure.JacksonTestFailureExpected;

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
    static class MergeListCustom {
        @JsonMerge
        @JsonProperty
        public MyListCustom<String> values = createCustom();
        { values.add("a"); }
    }

    static <T> MyListCustom<T> createCustom() {
        return new MyArrayListCustom<T>();
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void testJDKMapperReading() throws Exception {
        MergeListJDK result = MAPPER.readValue("{\"values\":[\"x\"]}", MergeListJDK.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
    }

    @JacksonTestFailureExpected
    @Test
    void testCustomMapperReading() throws Exception {
        MergeListCustom result = MAPPER.readValue("{\"values\":[\"x\"]}", MergeListCustom.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
    }

}

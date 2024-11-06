package com.fasterxml.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [databind#4783] Test to verify that JsonMerge also works for custom list
public class JsonMergeWithCustomCollection4783Test {

    static class CustomList4783<T> extends ArrayList<T> {
        static int constructorCount = 0;

        public CustomList4783() {
            if (constructorCount > 0) {
                throw new Error("Should only be called once");
            }
            ++constructorCount;
        }

        public static void reset() {
            constructorCount = 0;
        }
    }

    static class MergeList4783 {
        @JsonMerge
        public List<String> values;

        public MergeList4783() {
            this.values = new CustomList4783<>();
            this.values.add("a");
        }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    void testSimpleMergingWithCustomCollection() throws Exception {
        _verifyMergeList(
                MAPPER.readerForUpdating(new MergeList4783())
        );
        CustomList4783.reset();

        _verifyMergeList(
                MAPPER.readerFor(MergeList4783.class)
                        .withValueToUpdate(new MergeList4783())
        );
        CustomList4783.reset();
    }

    private void _verifyMergeList(ObjectReader reader) throws Exception {
        MergeList4783 result = reader.readValue("{\"values\":[\"x\"]}", MergeList4783.class);

        assertEquals(2, result.values.size());
        assertTrue(result.values.contains("x"));
        assertTrue(result.values.contains("a"));
        assertTrue(CustomList4783.constructorCount == 1);
    }
}

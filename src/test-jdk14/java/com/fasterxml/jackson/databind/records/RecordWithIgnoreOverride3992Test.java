package com.fasterxml.jackson.databind.records;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RecordWithIgnoreOverride3992Test extends BaseMapTest
{
    // [databind#3992]
    public record HelloRecord(String text, @JsonIgnore Recursion hidden) {
        // Before fix: works if this override is removed
        // After fix: works either way
        @Override
        public Recursion hidden() {
            return hidden;
        }
    }

    static class Recursion {
        public List<Recursion> all = new ArrayList<>();

        void add(Recursion recursion) {
            all.add(recursion);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // [databind#3992]
    public void testHelloRecord() throws Exception {
        Recursion beanWithRecursion = new Recursion();
        beanWithRecursion.add(beanWithRecursion);
        String json = MAPPER.writer()
                .writeValueAsString(new HelloRecord("hello", beanWithRecursion));
        assertEquals(a2q("{'text':'hello'}"), json);

        // Let's check deserialization works too, just in case.
        HelloRecord result = MAPPER.readValue(json, HelloRecord.class);
        assertNotNull(result);
    }
}

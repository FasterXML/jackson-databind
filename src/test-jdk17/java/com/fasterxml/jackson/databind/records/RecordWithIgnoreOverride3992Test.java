package com.fasterxml.jackson.databind.records;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecordWithIgnoreOverride3992Test extends DatabindTestUtil
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
    @Test
    public void testHelloRecord3992() throws Exception {
        Recursion beanWithRecursion = new Recursion();
        beanWithRecursion.add(beanWithRecursion);
        String json = MAPPER.writer()
                .writeValueAsString(new HelloRecord("hello", beanWithRecursion));
        assertEquals(a2q("{'text':'hello'}"), json);

        // Let's check deserialization works too, just in case.
        HelloRecord result = MAPPER.readValue(json, HelloRecord.class);
        assertNotNull(result);
    }

    // [databind#4626]
    @Test
    public void testDeserializeWithOverride4626() throws Exception {
        HelloRecord expected = new HelloRecord("hello", null);

        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello'}"), HelloRecord.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello','hidden':null}"), HelloRecord.class));
        assertEquals(expected, MAPPER.readValue(a2q("{'text':'hello','hidden':{'all': []}}"), HelloRecord.class));
    }
}

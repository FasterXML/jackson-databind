package com.fasterxml.jackson.databind.struct;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.BaseMapTest.newJsonMapper;

/**
 * Test to verify that [databind#3277] is fixed.
 */
public class UnwrappedDoubleWithAnySetter3277Test
{
    static class Holder {
        Object value1;

        @JsonUnwrapped
        Holder2 holder2;

        public Object getValue1() {
            return value1;
        }

        public void setValue1(Object value1) {
            this.value1 = value1;
        }
    }

    static class Holder2 {
        Map<String, Object> data = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getData() {
            return data;
        }

        @JsonAnySetter
        public void setAny(String key, Object value) {
            data.put(key, value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testIsInstanceOfDouble() throws Exception
    {
        Holder holder = MAPPER.readValue("{\"value1\": -60.0, \"value2\": -60.0}", Holder.class);

        // Validate type
        assertEquals(Double.class, holder.value1.getClass());
        assertEquals(Double.class, holder.holder2.data.get("value2").getClass());
        // Validate value
        assertEquals(-60.0, holder.value1);
        assertEquals(-60.0, holder.holder2.data.get("value2"));
    }
}

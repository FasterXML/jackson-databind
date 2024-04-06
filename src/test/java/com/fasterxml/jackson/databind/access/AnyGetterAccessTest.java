package com.fasterxml.jackson.databind.access;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Separate tests located in different package than code being
 * exercised; needed to trigger some access-related failures.
 */
public class AnyGetterAccessTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class DynaBean {
        public int id;

        protected Map<String,String> other = new HashMap<String,String>();

        @JsonAnyGetter
        public Map<String,String> any() {
            return other;
        }

        @JsonAnySetter
        public void set(String name, String value) {
            other.put(name, value);
        }
    }

    static class PrivateThing
    {
        @JsonAnyGetter
        public Map<?,?> getProperties()
        {
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("a", "A");
            return map;
        }
    }

    /*
    /**********************************************************
    /* Test cases
    /**********************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Test
    public void testDynaBean() throws Exception
    {
        DynaBean b = new DynaBean();
        b.id = 123;
        b.set("name", "Billy");
        assertEquals("{\"id\":123,\"name\":\"Billy\"}", MAPPER.writeValueAsString(b));

        DynaBean result = MAPPER.readValue("{\"id\":2,\"name\":\"Joe\"}", DynaBean.class);
        assertEquals(2, result.id);
        assertEquals("Joe", result.other.get("name"));
    }

    @Test
    public void testPrivate() throws Exception
    {
        assertEquals("{\"a\":\"A\"}",
                MAPPER.writeValueAsString(new PrivateThing()));
    }
}

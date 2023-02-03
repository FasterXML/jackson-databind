package com.fasterxml.jackson.databind.access;

import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

/**
 * Separate tests located in different package than code being
 * exercised; needed to trigger some access-related failures.
 */
public class TestAnyGetterAccess
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
     */

    static class DynaBean {
        public int id;

        protected HashMap<String,String> other = new HashMap<String,String>();

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

    private final ObjectMapper MAPPER = newJsonMapper();

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

    public void testPrivate() throws Exception
    {
        String json = MAPPER.writeValueAsString(new PrivateThing());
        assertEquals("{\"a\":\"A\"}", json);
    }
}

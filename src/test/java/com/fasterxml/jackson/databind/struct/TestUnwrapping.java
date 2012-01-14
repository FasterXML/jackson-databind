package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for verifying [JACKSON-132] implementation.
 */
public class TestUnwrapping extends BaseMapTest
{
    static class Unwrapping {
        public String name;
        @JsonUnwrapped
        public Location location;

        public Unwrapping() { }
        public Unwrapping(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    static class UnwrappingWithCreator {
        public String name;

        @JsonUnwrapped
        public Location location;

        @JsonCreator
        public UnwrappingWithCreator(@JsonProperty("name") String n) {
            name = n;
        }
    }
    
    static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Class with two unwrapped properties
    static class TwoUnwrappedProperties {
        @JsonUnwrapped
        public Location location;
        @JsonUnwrapped
        public Name name;

        public TwoUnwrappedProperties() { }
    }

    static class Name {
        public String first, last;
    }

    static class DeepUnwrapping
    {
        @JsonUnwrapped
        public Unwrapping unwrapped;

        public DeepUnwrapping() { }
        public DeepUnwrapping(String str, int x, int y) {
            unwrapped = new Unwrapping(str, x, y);
        }
    }

    // Class with unwrapping using prefixes
    static class PrefixUnwrap
    {
        public String name;
        @JsonUnwrapped(prefix="_")
        public Location location;

        public PrefixUnwrap() { }
        public PrefixUnwrap(String str, int x, int y) {
            name = str;
            location = new Location(x, y);
        }
    }

    static class DeepPrefixUnwrap
    {
        @JsonUnwrapped(prefix="u.")
        public PrefixUnwrap unwrapped;

        public DeepPrefixUnwrap() { }
        public DeepPrefixUnwrap(String str, int x, int y) {
            unwrapped = new PrefixUnwrap(str, x, y);
        }
    }
    
    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testSimpleUnwrappingSerialize() throws Exception
    {
        assertEquals("{\"name\":\"Tatu\",\"x\":1,\"y\":2}",
                mapper.writeValueAsString(new Unwrapping("Tatu", 1, 2)));
    }

    public void testPrefixedUnwrappingSerialize() throws Exception
    {
        assertEquals("{\"name\":\"Tatu\",\"_x\":1,\"_y\":2}",
                mapper.writeValueAsString(new PrefixUnwrap("Tatu", 1, 2)));
    }
    
    public void testDeepUnwrappingSerialize() throws Exception
    {
        assertEquals("{\"name\":\"Tatu\",\"x\":1,\"y\":2}",
                mapper.writeValueAsString(new DeepUnwrapping("Tatu", 1, 2)));
    }

    // 13-Jan-2012, tatu: sorta kinda works; but not 100% sure it's like it
    //    really SHOULD work?
    public void testDeepPrefixedUnwrappingSerialize() throws Exception
    {
        assertEquals("{\"u.name\":\"Bubba\",\"_x\":1,\"_y\":1}",
                mapper.writeValueAsString(new DeepPrefixUnwrap("Bubba", 1, 1)));
    }
    
    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */
    
    public void testSimpleUnwrappedDeserialize() throws Exception
    {
        Unwrapping bean = mapper.readValue("{\"name\":\"Tatu\",\"y\":7,\"x\":-13}",
                Unwrapping.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(-13, loc.x);
        assertEquals(7, loc.y);
    }
    
    public void testDoubleUnwrapping() throws Exception
    {
        TwoUnwrappedProperties bean = mapper.readValue("{\"first\":\"Joe\",\"y\":7,\"last\":\"Smith\",\"x\":-13}",
                TwoUnwrappedProperties.class);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(-13, loc.x);
        assertEquals(7, loc.y);
        Name name = bean.name;
        assertNotNull(name);
        assertEquals("Joe", name.first);
        assertEquals("Smith", name.last);
    }
    
    public void testDeepUnwrapping() throws Exception
    {
        DeepUnwrapping bean = mapper.readValue("{\"x\":3,\"name\":\"Bob\",\"y\":27}",
                DeepUnwrapping.class);
        Unwrapping uw = bean.unwrapped;
        assertNotNull(uw);
        assertEquals("Bob", uw.name);
        Location loc = uw.location;
        assertNotNull(loc);
        assertEquals(3, loc.x);
        assertEquals(27, loc.y);
    }
    
    public void testUnwrappedDeserializeWithCreator() throws Exception
    {
        UnwrappingWithCreator bean = mapper.readValue("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                UnwrappingWithCreator.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(1, loc.x);
        assertEquals(2, loc.y);
    }

    public void testPrefixedUnwrapping() throws Exception
    {
        PrefixUnwrap bean = mapper.readValue("{\"name\":\"Axel\",\"_x\":4,\"_y\":7}", PrefixUnwrap.class);
        assertNotNull(bean);
        assertEquals("Axel", bean.name);
        assertNotNull(bean.location);
        assertEquals(4, bean.location.x);
        assertEquals(7, bean.location.y);
    }
}

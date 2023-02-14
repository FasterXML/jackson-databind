package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Unit tests for verifying that basic {@link JsonUnwrapped} annotation
 * handling works as expected; some more advanced tests are separated out
 * to more specific test classes (like prefix/suffix handling).
 */
public class TestUnwrapped extends BaseMapTest
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

    final static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
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

    static class UnwrappingWithCreator {
        public String name;

        @JsonUnwrapped
        public Location location;

        @JsonCreator
        public UnwrappingWithCreator(@JsonProperty("name") String n) {
            name = n;
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

    // [databind#615]
    static class Parent {
        @JsonUnwrapped
        public Child c1;

        public Parent() { }
        public Parent(String str) { c1 = new Child(str); }
    }

    static class Child {
        public String field;

        public Child() { }
        public Child(String f) { field = f; }
    }

    static class Inner {
        public String animal;
    }

    static class Outer {
        @JsonUnwrapped
        Inner inner;
    }

    // [databind#1493]: case-insensitive handling
    static class Person {
        @JsonUnwrapped(prefix = "businessAddress.")
        public Address businessAddress;
    }

    static class Address {
        public String street;
        public String addon;
        public String zip;
        public String town;
        public String country;
    }

    // [databind#2088]
    static class Issue2088Bean {
        int x;
        int y;

        @JsonUnwrapped
        Issue2088UnwrappedBean w;

        public Issue2088Bean(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }

        public void setW(Issue2088UnwrappedBean w) {
            this.w = w;
        }
    }

    static class Issue2088UnwrappedBean {
        int a;
        int b;

        public Issue2088UnwrappedBean(@JsonProperty("a") int a, @JsonProperty("b") int b) {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************
    /* Tests, serialization
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleUnwrappingSerialize() throws Exception {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new Unwrapping("Tatu", 1, 2)));
    }

    public void testDeepUnwrappingSerialize() throws Exception {
        JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
        assertEquals("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                mapper.writeValueAsString(new DeepUnwrapping("Tatu", 1, 2)));
    }

    /*
    /**********************************************************
    /* Tests, deserialization
    /**********************************************************
     */

    public void testSimpleUnwrappedDeserialize() throws Exception
    {
        Unwrapping bean = MAPPER.readValue("{\"name\":\"Tatu\",\"y\":7,\"x\":-13}",
                Unwrapping.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(-13, loc.x);
        assertEquals(7, loc.y);
    }

    public void testDoubleUnwrapping() throws Exception
    {
        TwoUnwrappedProperties bean = MAPPER.readValue("{\"first\":\"Joe\",\"y\":7,\"last\":\"Smith\",\"x\":-13}",
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
        DeepUnwrapping bean = MAPPER.readValue("{\"x\":3,\"name\":\"Bob\",\"y\":27}",
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
        UnwrappingWithCreator bean = MAPPER.readValue("{\"x\":1,\"y\":2,\"name\":\"Tatu\"}",
                UnwrappingWithCreator.class);
        assertEquals("Tatu", bean.name);
        Location loc = bean.location;
        assertNotNull(loc);
        assertEquals(1, loc.x);
        assertEquals(2, loc.y);
    }

    public void testIssue615() throws Exception
    {
        Parent input = new Parent("name");
        String json = MAPPER.writeValueAsString(input);
        Parent output = MAPPER.readValue(json, Parent.class);
        assertEquals("name", output.c1.field);
    }

    public void testUnwrappedAsPropertyIndicator() throws Exception
    {
        Inner inner = new Inner();
        inner.animal = "Zebra";

        Outer outer = new Outer();
        outer.inner = inner;

        String actual = MAPPER.writeValueAsString(outer);

        assertTrue(actual.contains("animal"));
        assertTrue(actual.contains("Zebra"));
        assertFalse(actual.contains("inner"));
    }

    // [databind#1493]: case-insensitive handling
    public void testCaseInsensitiveUnwrap() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .build();
        Person p = mapper.readValue("{ }", Person.class);
        assertNotNull(p);
    }

    // [databind#2088]: accidental skipping of values
    public void testIssue2088UnwrappedFieldsAfterLastCreatorProp() throws Exception
    {
        Issue2088Bean bean = MAPPER.readValue("{\"x\":1,\"a\":2,\"y\":3,\"b\":4}", Issue2088Bean.class);
        assertEquals(1, bean.x);
        assertEquals(2, bean.w.a);
        assertEquals(3, bean.y);
        assertEquals(4, bean.w.b);
    }
}

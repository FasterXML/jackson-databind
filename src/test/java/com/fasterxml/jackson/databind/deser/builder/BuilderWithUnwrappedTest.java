package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

public class BuilderWithUnwrappedTest extends BaseMapTest
{
    // // // Builder with unwrapped stuff

    final static class Location {
        public int x;
        public int y;

        public Location() { }
        public Location(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    @JsonDeserialize(builder=UnwrappingBuilder.class)
    static class UnwrappingValue
    {
        final String name;
        final Location location;
        final String stuff;

        public UnwrappingValue(String n, Location l, String st) {
            name = n;
            location = l;
            stuff = st;
        }
    }

    static class UnwrappingBuilder
    {
        private String name;

        Location loc;

        @JacksonInject
        protected String stuff;
        
        @JsonUnwrapped(prefix="loc.")
        public UnwrappingBuilder withLocation(Location l) {
            loc = l;
            return this;
        }

        public UnwrappingBuilder withName(String n) {
            name = n;
            return this;
        }
        
        public UnwrappingValue build() {
            return new UnwrappingValue(name, loc, stuff);
        }
    }

    @JsonDeserialize(builder=UnwrappingCreatorBuilder.class)
    static class UnwrappingCreatorValue
    {
        final String name;
        final Location location;
        final String stuff;

        public UnwrappingCreatorValue(String n, Location l, String st) {
            name = n;
            location = l;
            stuff = st;
        }
    }

    static class UnwrappingCreatorBuilder
    {
        private String name;

        Location loc;

        @JacksonInject
        protected String stuff;
        
        
        public UnwrappingCreatorBuilder(@JsonProperty("name") String name,
                @JsonUnwrapped(prefix="loc.") Location l
                ) {
            loc = l;
        }

        public UnwrappingCreatorValue build() {
            return new UnwrappingCreatorValue(name, loc, stuff);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testWithUnwrapping() throws Exception
    {
        final String json = aposToQuotes("{'loc.x':3,'name':'Foobar','loc.y':5}}");

        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
                .addValue(String.class, "stuffValue")
                );
        
        UnwrappingValue result = mapper.readValue(json, UnwrappingValue.class);
        assertNotNull(result);
        assertNotNull(result.location);
        assertEquals("Foobar", result.name);
        assertEquals(3, result.location.x);
        assertEquals(5, result.location.y);
        assertEquals("stuffValue", result.stuff);

        ObjectReader r = MAPPER.readerFor(UnwrappingValue.class)
                .withValueToUpdate(new UnwrappingValue("foo", new Location(1, 2), null));
        // 30-Nov-2016, tatu: Actually, updateValue() NOT supported, verify:
        try {
            result = r.readValue(json);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "existing instance");
        }
    }

    // Alas: can't pass, until [databind#265] fixed:
    // 23-Feb-2017, tatu: or its follow-up: error message is now more descriptive...
    public void testWithCreatorUnwrapping() throws Exception
    {
        final String json = aposToQuotes("{'loc.x':4,'name':'Foobar','loc.y': 7}}");

        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
                .addValue(String.class, "stuffValue")
                );

        @SuppressWarnings("unused")
        UnwrappingCreatorValue result;
        try {
            result = mapper.readValue(json, UnwrappingCreatorValue.class);
            fail("Did not expect to really pass -- should maybe update the test");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "combination not yet supported");
        }

        /*
        assertNotNull(result);
        assertNotNull(result.location);
        assertEquals("Foobar", result.name);
        assertEquals(4, result.location.x);
        assertEquals(7, result.location.y);
        */
    }
}

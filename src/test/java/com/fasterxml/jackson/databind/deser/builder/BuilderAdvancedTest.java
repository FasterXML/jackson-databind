package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderAdvancedTest extends BaseMapTest
{
    @JsonDeserialize(builder=InjectableBuilderXY.class)
    static class InjectableXY
    {
        final int _x, _y;
        final String _stuff;

        protected InjectableXY(int x, int y, String stuff) {
            _x = x+1;
            _y = y+1;
            _stuff = stuff;
        }
    }

    static class InjectableBuilderXY
    {
        public int x, y;

        @JacksonInject
        protected String stuff;
        
        public InjectableBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        public InjectableBuilderXY withY(int y0) {
              this.y = y0;
              return this;
        }

        public InjectableXY build() {
              return new InjectableXY(x, y, stuff);
        }
    }

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

        protected UnwrappingValue(String n, Location l) {
            name = n;
            location = l;
        }
    }

    static class UnwrappingBuilder
    {
        private String name;

        Location loc;

        @JsonUnwrapped
        public UnwrappingBuilder withLocation(Location l) {
            loc = l;
            return this;
        }

        public UnwrappingBuilder withName(String n) {
            name = n;
            return this;
        }
        
        public UnwrappingValue build() {
            return new UnwrappingValue(name, loc);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testWithInjectable() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "stuffValue")
            );
        InjectableXY bean = mapper.readValue(aposToQuotes("{'y':3,'x':7}"),
                InjectableXY.class);
        assertEquals(8, bean._x);
        assertEquals(4, bean._y);
        assertEquals("stuffValue", bean._stuff);
    }

    public void testWithUnwrapping() throws Exception
    {
        final String json = aposToQuotes("{'x':3,'name':'Foobar','y':5}}");
        UnwrappingValue result = MAPPER.readValue(json, UnwrappingValue.class);
        assertNotNull(result);
        assertNotNull(result.location);
        assertEquals("Foobar", result.name);
        assertEquals(3, result.location.x);
        assertEquals(5, result.location.y);
    }
}

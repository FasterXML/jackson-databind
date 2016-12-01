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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
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
}

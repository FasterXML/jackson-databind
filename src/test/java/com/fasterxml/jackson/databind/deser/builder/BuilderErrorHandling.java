package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class BuilderErrorHandling extends BaseMapTest
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    static class SimpleBuilderXY
    {
        int x, y;
     
        public SimpleBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        public SimpleBuilderXY withY(int y0) {
              this.y = y0;
              return this;
        }

        public ValueClassXY build() {
              return new ValueClassXY(x, y);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testUnknownProperty() throws Exception
    {
        // first, default failure
        String json = aposToQuotes("{'x':1,'z':2,'y':4}");
        try {
            MAPPER.readValue(json, ValueClassXY.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "unrecognized field");
        }
        // but pass if ok to ignore
        ValueClassXY result = MAPPER.readerFor(ValueClassXY.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertEquals(2, result._x);
        assertEquals(5, result._y);
    }
}

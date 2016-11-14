package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderWithViewTest extends BaseMapTest
{
    static class ViewX { }
    static class ViewY { }

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
        public int x, y;

        @JsonView(ViewX.class)
        public SimpleBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        @JsonView(ViewY.class)
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

    public void testSimpleViews() throws Exception
    {
        final String json = aposToQuotes("{'x':5,'y':10}");
        ValueClassXY resultX = MAPPER.readerFor(ValueClassXY.class)
                .withView(ViewX.class)
                .readValue(json);
        assertEquals(6, resultX._x);
        assertEquals(1, resultX._y);

        ValueClassXY resultY = MAPPER.readerFor(ValueClassXY.class)
                .withView(ViewY.class)
                .readValue(json);
        assertEquals(1, resultY._x);
        assertEquals(11, resultY._y);
    }
}

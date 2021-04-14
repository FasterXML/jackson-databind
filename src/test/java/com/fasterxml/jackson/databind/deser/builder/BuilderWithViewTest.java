package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.*;

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

    @JsonDeserialize(builder=CreatorBuilderXY.class)
    static class CreatorValueXY
    {
        final int _x, _y;

        protected CreatorValueXY(int x, int y) {
            _x = x;
            _y = y;
        }
    }

    @JsonIgnoreProperties({ "bogus" })
    static class CreatorBuilderXY
    {
        public int x, y;

        @JsonCreator
        public CreatorBuilderXY(@JsonProperty("x") @JsonView(ViewX.class) int x,
                @JsonProperty("y") @JsonView(ViewY.class) int y)
        {
            this.x = x;
            this.y = y;
        }

        public CreatorValueXY build() {
              return new CreatorValueXY(x, y);
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
        final String json = a2q("{'x':5,'y':10}");
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

    public void testCreatorViews() throws Exception
    {
        final String json = a2q("{'x':5,'y':10,'bogus':false}");
        CreatorValueXY resultX = MAPPER.readerFor(CreatorValueXY.class)
                .withView(ViewX.class)
                .readValue(json);
        assertEquals(5, resultX._x);
        assertEquals(0, resultX._y);

        CreatorValueXY resultY = MAPPER.readerFor(CreatorValueXY.class)
                .withView(ViewY.class)
                .readValue(json);
        assertEquals(0, resultY._x);
        assertEquals(10, resultY._y);
    }
}

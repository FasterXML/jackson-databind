package com.fasterxml.jackson.databind.struct;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Unit tests for "POJO as array" feature using Builder-style
 * POJO construction.
 */
public class TestPOJOAsArrayWithBuilder extends BaseMapTest
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class SimpleBuilderXY
    {
        public int x, y;

        protected SimpleBuilderXY() { }
        protected SimpleBuilderXY(int x0, int y0) {
            x = x0;
            y = y0;
        }

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

    // Also, with creator:

    @JsonDeserialize(builder=CreatorBuilder.class)
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class CreatorValue
    {
        final int a, b, c;

        protected CreatorValue(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    static class CreatorBuilder {
        private final int a, b;

        private int c;

        @JsonCreator
        public CreatorBuilder(@JsonProperty("a") int a,
                @JsonProperty("b") int b)
        {
            this.a = a;
            this.b = b;
        }

        @JsonView(String.class)
        public CreatorBuilder withC(int v) {
            c = v;
            return this;
        }

        public CreatorValue build() {
            return new CreatorValue(a, b, c);
        }
    }

    /*
    /*****************************************************
    /* Basic tests
    /*****************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();

    public void testSimpleBuilder() throws Exception
    {
        // Ok, first, ensure that serializer will "black out" filtered properties
        ValueClassXY value = MAPPER.readValue("[1,2]", ValueClassXY.class);
        assertEquals(2, value._x);
        assertEquals(3, value._y);
    }

    // Won't work, but verify exception
    public void testBuilderWithUpdate() throws Exception
    {
        // Ok, first, simple case of all values being present
        try {
            /*value =*/ MAPPER.readerFor(ValueClassXY.class)
                    .withValueToUpdate(new ValueClassXY(6, 7))
                    .readValue("[1,2]");
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Deserialization of");
            verifyException(e, "by passing existing instance");
            verifyException(e, "ValueClassXY");
        }
    }

    /*
    /*****************************************************
    /* Creator test(s)
    /*****************************************************
     */

    // test to ensure @JsonCreator also works
    public void testWithCreator() throws Exception
    {
        CreatorValue value = MAPPER.readValue("[1,2,3]", CreatorValue.class);
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);

        // and should be ok with partial too?
        value = MAPPER.readValue("[1,2]", CreatorValue.class);
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(0, value.c);

        value = MAPPER.readValue("[1]", CreatorValue.class);
        assertEquals(1, value.a);
        assertEquals(0, value.b);
        assertEquals(0, value.c);

        value = MAPPER.readValue("[]", CreatorValue.class);
        assertEquals(0, value.a);
        assertEquals(0, value.b);
        assertEquals(0, value.c);
    }

    public void testWithCreatorAndView() throws Exception
    {
        ObjectReader reader = MAPPER.readerFor(CreatorValue.class);
        CreatorValue value;

        // First including values in view
        value = reader.withView(String.class).readValue("[1,2,3]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(3, value.c);

        // then not including view
        value = reader.withView(Character.class).readValue("[1,2,3]");
        assertEquals(1, value.a);
        assertEquals(2, value.b);
        assertEquals(0, value.c);
    }

    /*
    /*****************************************************
    /* Failure tests
    /*****************************************************
     */

    public void testUnknownExtraProp() throws Exception
    {
        String json = "[1, 2, 3, 4]";
        try {
            MAPPER.readValue(json, ValueClassXY.class);
            fail("should not pass with extra element");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unexpected JSON values");
        }

        // but actually fine if skip-unknown set
        ValueClassXY v = MAPPER.readerFor(ValueClassXY.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(json);
        assertNotNull(v);
        // note: +1 for both so
        assertEquals(v._x, 2);
        assertEquals(v._y, 3);
    }
}

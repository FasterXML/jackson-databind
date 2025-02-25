package com.fasterxml.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class TestPOJOAsArrayAdvanced extends DatabindTestUtil
{
    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class CreatorAsArray
    {
        protected int x, y;
        public int a, b;

        @JsonCreator
        public CreatorAsArray(@JsonProperty("x") int x, @JsonProperty("y") int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"a","b","x","y"})
    static class CreatorAsArrayShuffled
    {
        protected int x, y;
        public int a, b;

        @JsonCreator
        public CreatorAsArrayShuffled(@JsonProperty("x") int x, @JsonProperty("y") int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX() { return x; }
        public int getY() { return y; }
    }

    static class ViewA { }
    static class ViewB { }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class AsArrayWithView
    {
        @JsonView(ViewA.class)
        public int a;
        @JsonView(ViewB.class)
        public int b;
        public int c;
    }

    @JsonFormat(shape=JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder(alphabetic=true)
    static class AsArrayWithViewAndCreator
    {
        @JsonView(ViewA.class)
        public int a;
        @JsonView(ViewB.class)
        public int b;
        public int c;

        @JsonCreator
        public AsArrayWithViewAndCreator(@JsonProperty("a") int a,
                @JsonProperty("b") int b,
                @JsonProperty("c") int c)
        {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    /*
    /*****************************************************
    /* Basic tests
    /*****************************************************
     */

    // 06-Jan-2025, tatu: NOTE! need to make sure Default View Inclusion
    //   is enabled for tests to work as expected
    private final static ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .build();

    @Test
    public void testWithView() throws Exception
    {
        // Ok, first, ensure that serializer will "black out" filtered properties
        AsArrayWithView input = new AsArrayWithView();
        input.a = 1;
        input.b = 2;
        input.c = 3;
        String json = MAPPER.writerWithView(ViewA.class).writeValueAsString(input);
        assertEquals("[1,null,3]", json);

        // and then that conversely deserializer does something similar
        AsArrayWithView result = MAPPER.readerFor(AsArrayWithView.class).withView(ViewB.class)
                .readValue("[1,2,3]");
        // should include 'c' (not view-able) and 'b' (include in ViewB) but not 'a'
        assertEquals(3, result.c);
        assertEquals(2, result.b);
        assertEquals(0, result.a);
    }

    @Test
    public void testWithViewAndCreator() throws Exception
    {
        AsArrayWithViewAndCreator result = MAPPER.readerFor(AsArrayWithViewAndCreator.class)
                .withView(ViewB.class)
                .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .readValue("[1,2,3]");
        // should include 'c' (not view-able) and 'b' (include in ViewB) but not 'a'
        assertEquals(3, result.c);
        assertEquals(2, result.b);
        assertEquals(0, result.a);
    }

    @Test
    public void testWithCreatorsOrdered() throws Exception
    {
        CreatorAsArray input = new CreatorAsArray(3, 4);
        input.a = 1;
        input.b = 2;

        // note: Creator properties get sorted ahead of others, hence not [1,2,3,4] but:
        String json = MAPPER.writeValueAsString(input);
        assertEquals("[3,4,1,2]", json);

        // and should get back in proper order, too
        CreatorAsArray output = MAPPER.readValue(json, CreatorAsArray.class);
        assertEquals(1, output.a);
        assertEquals(2, output.b);
        assertEquals(3, output.x);
        assertEquals(4, output.y);
    }

    // Same as above, but ordering of properties different...
    @Test
    public void testWithCreatorsShuffled() throws Exception
    {
        CreatorAsArrayShuffled input = new CreatorAsArrayShuffled(3, 4);
        input.a = 1;
        input.b = 2;

        // note: explicit ordering overrides implicit creators-first ordering:
        String json = MAPPER.writeValueAsString(input);
        assertEquals("[1,2,3,4]", json);

        // and should get back in proper order, too
        CreatorAsArrayShuffled output = MAPPER.readValue(json, CreatorAsArrayShuffled.class);
        assertEquals(1, output.a);
        assertEquals(2, output.b);
        assertEquals(3, output.x);
        assertEquals(4, output.y);
    }
}

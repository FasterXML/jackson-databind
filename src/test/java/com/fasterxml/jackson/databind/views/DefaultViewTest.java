package com.fasterxml.jackson.databind.views;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [databind#507], supporting default views
public class DefaultViewTest extends DatabindTestUtil
{
    // Classes that represent views
    static class ViewA { }
    static class ViewAA extends ViewA { }
    static class ViewB { }
    static class ViewBB extends ViewB { }

    @JsonView(ViewA.class)
    @JsonPropertyOrder({ "a", "b" })
    static class Defaulting {
        public int a = 3;

        @JsonView(ViewB.class)
        public int b = 5;
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testDeserialization() throws IOException
    {
        final String JSON = a2q("{'a':1,'b':2}");

        // first: no views:
        Defaulting result = MAPPER.readerFor(Defaulting.class)
                .readValue(JSON);
        assertEquals(result.a, 1);
        assertEquals(result.b, 2);

        // Then views; first A, then B(B)
        result = MAPPER.readerFor(Defaulting.class)
                .withView(ViewA.class)
                .readValue(JSON);
        assertEquals(result.a, 1);
        assertEquals(result.b, 5);

        result = MAPPER.readerFor(Defaulting.class)
                .withView(ViewBB.class)
                .readValue(JSON);
        assertEquals(result.a, 3);
        assertEquals(result.b, 2);
    }

    @Test
    public void testSerialization() throws IOException
    {
        assertEquals(a2q("{'a':3,'b':5}"),
                MAPPER.writeValueAsString(new Defaulting()));

        assertEquals(a2q("{'a':3}"),
                MAPPER.writerWithView(ViewA.class)
                    .writeValueAsString(new Defaulting()));
        assertEquals(a2q("{'b':5}"),
                MAPPER.writerWithView(ViewB.class)
                    .writeValueAsString(new Defaulting()));
    }
}

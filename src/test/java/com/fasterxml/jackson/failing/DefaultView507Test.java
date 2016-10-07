package com.fasterxml.jackson.failing;

import java.io.IOException;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class DefaultView507Test extends BaseMapTest
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

    public void testSimple() throws IOException
    {
        assertEquals(aposToQuotes("{'a':3}"),
                MAPPER.writerWithView(ViewA.class)
                    .writeValueAsString(new Defaulting()));
        assertEquals(aposToQuotes("{'b':5}"),
                MAPPER.writerWithView(ViewB.class)
                    .writeValueAsString(new Defaulting()));
    }
}

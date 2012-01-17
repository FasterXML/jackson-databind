package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/* Tests mostly for [JACKSON-754]: ability to create "annotation bundles"
 */
public class TestAnnotionBundles extends com.fasterxml.jackson.databind.BaseMapTest
{
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonIgnore
    private @interface MyIgnoral { }

    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonProperty("foobar")
    private @interface MyRename { }

    protected final static class Bean {
        @MyIgnoral
        public String getIgnored() { return "foo"; }
 
        @MyRename
        public int renamed = 13;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testBundledIgnore() throws Exception
    {
        assertEquals("{\"foobar\":13}", mapper.writeValueAsString(new Bean()));
    }
}

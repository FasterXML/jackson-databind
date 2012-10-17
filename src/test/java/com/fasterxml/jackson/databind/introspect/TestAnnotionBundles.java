package com.fasterxml.jackson.databind.introspect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
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

    @Retention(RetentionPolicy.RUNTIME)
    @JsonAutoDetect(fieldVisibility=Visibility.NONE,
            getterVisibility=Visibility.NONE, isGetterVisibility=Visibility.NONE)
    @JacksonAnnotationsInside
    public @interface JsonAutoDetectOff {}

    @JsonAutoDetectOff
    public class NoAutoDetect {
      public int getA() { return 13; }
      
      @JsonProperty
      public int getB() { return 5; }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonProperty("_id")
    public @interface Bundle92 {}

    public class Bean92 {
        @Bundle92
        protected String id = "abc";
    }    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    public void testBundledIgnore() throws Exception
    {
        assertEquals("{\"foobar\":13}", MAPPER.writeValueAsString(new Bean()));
    }

    public void testVisibilityBundle() throws Exception
    {
        assertEquals("{\"b\":5}", MAPPER.writeValueAsString(new NoAutoDetect()));
    }
    
    public void testIssue92() throws Exception
    {
        assertEquals("{\"_id\":\"abc\"}", MAPPER.writeValueAsString(new Bean92()));
    }
}

package com.fasterxml.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#3647] : Support @JsonIgnoreProperties to work with @JsonValue
public class JsonValueIgnore3647Test extends DatabindTestUtil
{
    final static class Foo3647 {
        public String p1 = "hello";
        public String p2 = "world";
    }

    static class Bar3647 {
        @JsonValue
        @JsonIgnoreProperties("p1")
        public Foo3647 getFoo() {
            return new Foo3647();
        }
    }

    @JsonIgnoreProperties({"a"})
    static class Bean3647 {
        public String a = "hello";
        public String b = "world";
    }

    static class Container3647 {
        @JsonValue
        @JsonIgnoreProperties("b")
        public Bean3647 getBean() {
            return new Bean3647();
        }
    }

    static class Base3647 {
        public String a = "hello";
        public String b = "world";
    }

    static class BaseContainer3647 {
        @JsonValue
        public Base3647 getBean() {
            return new Base3647();
        }
    }
    
    static class MixinContainer3647 {
        @JsonIgnoreProperties("b")
        public Base3647 getBean() {
            return new Base3647();
        }
    }

    @JsonIgnoreProperties({"a", "b"})
    static class Mixin3647 {
        public String a = "hello";
        public String b = "world";
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testIgnorePropsAndJsonValueAtSameLevel() throws Exception
    {
        assertEquals("{\"p2\":\"world\"}", 
                MAPPER.writeValueAsString(new Bar3647()));
    }

    @Test
    public void testUnionOfIgnorals() throws Exception
    {
        assertEquals("{}", 
                MAPPER.writeValueAsString(new Container3647()));
    }

    @Test
    public void testMixinContainerAndJsonValue() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(BaseContainer3647.class, MixinContainer3647.class)
                .build();
        
        assertEquals("{\"a\":\"hello\"}", 
                mapper.writeValueAsString(new BaseContainer3647()));
    }

    @Test
    public void testMixinAndJsonValue() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .addMixIn(Base3647.class, Mixin3647.class)
                .build();
        
        assertEquals("{}", 
                mapper.writeValueAsString(new Base3647()));
    }
}

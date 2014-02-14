package com.fasterxml.jackson.failing;

import java.io.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.SimpleType;

public class TestMartinsExternalId extends BaseMapTest
{
    public void testExternal() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        Parent parent = mapper.readValue("{\"type\":\"a\",\"child\":{\"value\":\"foo\"}}", Parent.class);
        assertNotNull(parent);
    }
     
    @JsonTypeIdResolver(Resolver.class)
    public static class Parent
    {
        @JsonProperty
        public String type;

        @JsonTypeIdResolver(Resolver.class)
        @JsonProperty
        @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include= JsonTypeInfo.As.EXTERNAL_PROPERTY, property="type"
            , visible = true
        )
        public Child child;
    }

    public static interface Child { }

    @JsonTypeIdResolver(Resolver.class)
    public static class Child1 implements Child
    {
        @JsonProperty
        public String value;
        }
         
        public static class Child2
        implements Child
        {
        @JsonProperty
        public String value;
    }
     
     
    public static class Resolver implements TypeIdResolver
    {
        public Resolver() { }
        
        @Override
        public void init(JavaType baseType) { }
     
        @Override
        public String idFromValue(Object value)
        {
        if (value instanceof Child1) {
        return "a";
        }
        else if (value instanceof Child2) {
        return "b";
        }
         
        throw new IllegalArgumentException();
        }
     
        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType)
        {
            return idFromValue(value);
        }
         
        @Override
        public String idFromBaseType()
        {
            throw new UnsupportedOperationException();
        }
             
        @Override
        public JavaType typeFromId(String id)
        {
            if (id.equals("a")) {
                return SimpleType.construct(Child1.class);
            }
            else if (id.equals("b")) {
                return SimpleType.construct(Child2.class);
            }
            throw new IllegalArgumentException();
        }
     
        @Override
        public JsonTypeInfo.Id getMechanism()
        {
            return JsonTypeInfo.Id.CUSTOM;
        }
    }
}

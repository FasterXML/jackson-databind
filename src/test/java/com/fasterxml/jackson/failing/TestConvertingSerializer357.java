package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public class TestConvertingSerializer357
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    // [Issue#357]
    static class A { }

    static class B {
        @JsonSerialize(contentConverter = AToStringConverter.class)
        public List<A> list = Arrays.asList(new A());
    }

    static class AToStringConverter extends StdConverter<A, List<String>> {
        @Override
        public List<String> convert(A value) {
            return Arrays.asList("Hello world!");
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#357]
    public void testConverterForList357() throws Exception {
        String json = objectWriter().writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world!\"]]}", json);
    }
}

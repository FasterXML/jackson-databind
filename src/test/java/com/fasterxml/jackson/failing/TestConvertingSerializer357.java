package com.fasterxml.jackson.failing;

import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public class TestConvertingSerializer357
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    // [Issue#357]
    static class Value { }

    static class ListWrapper {
        @JsonSerialize(contentConverter = ValueToStringListConverter.class)
        public List<Value> list = Arrays.asList(new Value());
    }

    static class ValueToStringListConverter extends StdConverter<Value, List<String>> {
        @Override
        public List<String> convert(Value value) {
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
        String json = objectWriter().writeValueAsString(new ListWrapper());
        assertEquals("{\"list\":[[\"Hello world!\"]]}", json);
    }
}

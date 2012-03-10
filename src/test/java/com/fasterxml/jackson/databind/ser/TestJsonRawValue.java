package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This unit test suite tests functioning of {@link JsonRawValue}
 * annotation with bean serialization.
 */
public class TestJsonRawValue
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    /*
    /*********************************************************
    /* Helper bean classes
    /*********************************************************
     */

    /// Class for testing {@link JsonRawValue} annotations with getters returning String
    @JsonPropertyOrder(alphabetic=true)
    final static class ClassGetter<T>
    {
    	private final T _value;
    	
        private ClassGetter(T value) { _value = value;}
 
        public T getNonRaw() { return _value; }

        @JsonProperty("raw") @JsonRawValue public T foobar() { return _value; }
        
        @JsonProperty @JsonRawValue protected T value() { return _value; }
    }
    
    /*
    /*********************************************************
    /* Test cases
    /*********************************************************
     */

    public void testSimpleStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String value = "abc";
        String result = m.writeValueAsString(new ClassGetter<String>(value));
        String expected = String.format("{\"nonRaw\":\"%s\",\"raw\":%s,\"value\":%s}", value, value, value);
        assertEquals(expected, result);
    }

    public void testSimpleNonStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        int value = 123;
        String result = m.writeValueAsString(new ClassGetter<Integer>(value));
        String expected = String.format("{\"nonRaw\":%d,\"raw\":%d,\"value\":%d}", value, value, value);
        assertEquals(expected, result);
    }

    public void testNullStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String result = m.writeValueAsString(new ClassGetter<String>(null));
        String expected = "{\"nonRaw\":null,\"raw\":null,\"value\":null}";
        assertEquals(expected, result);
    }

}

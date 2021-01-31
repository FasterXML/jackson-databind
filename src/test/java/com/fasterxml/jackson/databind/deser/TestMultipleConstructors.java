package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMultipleConstructors extends BaseMapTest {
    static class MultipleConstructorClass {
        private final String message;
        private final int code;

        MultipleConstructorClass(String message, int code) {
            this.message = message;
            this.code = code;
        }

        MultipleConstructorClass(String message) {
            this(message, 0);
        }

        MultipleConstructorClass(int code) {
            this("", code);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testClassWithMultipleConstructors() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MultipleConstructorClass result1 = m.readValue("{ \"code\":3}", MultipleConstructorClass.class);
        assertEquals(3, result1.code);
        assertEquals("", result1.message);
        MultipleConstructorClass result2 = m.readValue("{ \"message\":\"test123\"}", MultipleConstructorClass.class);
        assertEquals(0, result1.code);
        assertEquals("test123", result1.message);
    }
}

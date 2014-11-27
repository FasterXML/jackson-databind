package com.fasterxml.jackson.databind.deser;


import javax.money.MonetaryAmount;

import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestNoClassDefFoundDeserializer extends BaseMapTest {

    public static class Parent {
        public List<Child> money;
    }

    public static class Child {
        public MonetaryAmount money;
    }

    public void testClassIsMissing() throws ClassNotFoundException
    {
        boolean missing = false;
        try {
            Class.forName("javax.money.MonetaryAmount");
        } catch (ClassNotFoundException ex) {
            missing = true;
        }
        assertTrue("javax.money.MonetaryAmount is not in classpath", missing);
    }

    public void testDeserialize() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Parent result = m.readValue(" { } ", Parent.class);
        assertNotNull(result);
    }

    public void testUseMissingClass() throws Exception
    {
        boolean missing = false;
        try {
            ObjectMapper m = new ObjectMapper();
            m.readValue(" { \"money\" : [{}] } ", Parent.class);
        } catch (NoClassDefFoundError ex) {
            missing = true;
        }
        assertTrue("cannot instantiate a missing class", missing);
    }

}

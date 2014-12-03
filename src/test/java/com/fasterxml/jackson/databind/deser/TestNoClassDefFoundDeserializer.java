package com.fasterxml.jackson.databind.deser;

import javax.measure.Measure;

import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestNoClassDefFoundDeserializer extends BaseMapTest {

    public static class Parent {
        public List<Child> child;
    }

    public static class Child {
        public Measure<?> measure;
    }

    public void testClassIsMissing()
    {
        boolean missing = false;
        try {
            Class.forName("javax.measure.Measure");
        } catch (ClassNotFoundException ex) {
            missing = true;
        }
        assertTrue("javax.measure.Measure is not in classpath", missing);
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
            m.readValue(" { \"child\" : [{}] } ", Parent.class);
        } catch (NoClassDefFoundError ex) {
            missing = true;
        }
        assertTrue("cannot instantiate a missing class", missing);
    }

}

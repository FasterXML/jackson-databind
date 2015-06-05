package com.fasterxml.jackson.databind.introspect;

import javax.measure.Measure;

import java.util.List;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for [databind#636]
public class NoClassDefFoundWorkaroundTest extends BaseMapTest
{
    public static class Parent {
        public List<Child> child;
    }

    public static class Child {
        public Measure<?> measure;
    }

    public void testClassIsMissing()
    {
        try {
            Class.forName("javax.measure.Measure");
            fail("Should not have found javax.measure.Measure");
        } catch (ClassNotFoundException ex) {
            ; // expected case
        }
    }

    public void testDeserialize() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Parent result = null;

        try {
            result = m.readValue(" { } ", Parent.class);
        } catch (Exception e) {
            fail("Should not have had issues, got: "+e);
        }
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

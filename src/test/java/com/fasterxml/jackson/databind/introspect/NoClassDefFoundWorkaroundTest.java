package com.fasterxml.jackson.databind.introspect;

import java.util.List;
import javax.measure.Measure;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#636]
public class NoClassDefFoundWorkaroundTest extends DatabindTestUtil
{
    public static class Parent {
        public List<Child> child;
    }

    public static class Child {
        public Measure<?> measure;
    }

    @Test
    public void testClassIsMissing()
    {
        try {
            Class.forName("javax.measure.Measure");
            fail("Should not have found javax.measure.Measure");
        } catch (ClassNotFoundException ex) {
            ; // expected case
        }
    }

    @Test
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

    @Test
    public void testUseMissingClass() throws Exception
    {
        boolean missing = false;
        try {
            ObjectMapper m = new ObjectMapper();
            m.readValue(" { \"child\" : [{}] } ", Parent.class);
        } catch (NoClassDefFoundError ex) {
            missing = true;
        }
        assertTrue(missing, "cannot instantiate a missing class");
    }
}

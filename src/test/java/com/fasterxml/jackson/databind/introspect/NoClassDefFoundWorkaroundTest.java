package com.fasterxml.jackson.databind.introspect;

import java.util.List;

import javax.measure.Measure;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [databind#636]
class NoClassDefFoundWorkaroundTest extends DatabindTestUtil
{
    public static class Parent {
        public List<Child> child;
    }

    public static class Child {
        public Measure<?> measure;
    }

    @Test
    void classIsMissing()
    {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("javax.measure.Measure"),
                "Should not have found javax.measure.Measure");
    }

    @Test
    void deserialize()
    {
        ObjectMapper m = new ObjectMapper();
        Parent result = assertDoesNotThrow(
                () -> m.readValue(" { } ", Parent.class),
                "Should not have had issues, got: ");
        assertNotNull(result);
    }

    @Test
    void useMissingClass()
    {
        ObjectMapper m = new ObjectMapper();
        assertThrows(
                NoClassDefFoundError.class,
                () -> m.readValue(" { \"child\" : [{}] } ", Parent.class),
                "cannot instantiate a missing class"
        );
    }
}

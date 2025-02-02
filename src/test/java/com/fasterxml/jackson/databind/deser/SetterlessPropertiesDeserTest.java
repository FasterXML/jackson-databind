package com.fasterxml.jackson.databind.deser;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

/**
 * Unit tests for verifying that feature requested
 * via [JACKSON-88] ("setterless collections") work as
 * expected, similar to how Collections and Maps work
 * with JAXB.
 */
public class SetterlessPropertiesDeserTest
{
    static class CollectionBean
    {
        List<String> _values = new ArrayList<>();

        public List<String> getValues() { return _values; }
    }

    static class MapBean
    {
        Map<String,Integer> _values = new HashMap<>();

        public Map<String,Integer> getValues() { return _values; }
    }

    // testing to verify that field has precedence over getter, for lists
    static class Dual
    {
        @JsonProperty("list") protected List<Integer> values = new ArrayList<>();

        public Dual() { }

        public List<Integer> getList() {
            throw new IllegalStateException("Should not get called");
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testSimpleSetterlessCollectionOk()
        throws Exception
    {
        CollectionBean result = new ObjectMapper().readValue
            ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
        List<String> l = result._values;
        assertEquals(2, l.size());
        assertEquals("abc", l.get(0));
        assertEquals("def", l.get(1));
    }

    /**
     * Let's also verify that disabling the feature makes
     * deserialization fail for setterless bean
     */
    @Test
    public void testSimpleSetterlessCollectionFailure()
        throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // by default, it should be enabled
        assertTrue(m.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));
        m = jsonMapperBuilder()
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
                .build();
        assertFalse(m.isEnabled(MapperFeature.USE_GETTERS_AS_SETTERS));

        // and now this should fail
        try {
            m.readValue
                ("{\"values\":[ \"abc\", \"def\" ]}", CollectionBean.class);
            fail("Expected an exception");
        } catch (MismatchedInputException e) {
            // Not a good exception, ideally could suggest a need for
            // a setter...?
            verifyException(e, "Unrecognized field");
        }
    }

    @Test
    public void testSimpleSetterlessMapOk()
        throws Exception
    {
        MapBean result = new ObjectMapper().readValue
            ("{\"values\":{ \"a\": 15, \"b\" : -3 }}", MapBean.class);
        Map<String,Integer> m = result._values;
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(15), m.get("a"));
        assertEquals(Integer.valueOf(-3), m.get("b"));
    }

    @Test
    public void testSimpleSetterlessMapFailure()
        throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)
                .build();
        // so this should fail now without a setter
        try {
            m.readValue
                ("{\"values\":{ \"a\":3 }}", MapBean.class);
            fail("Expected an exception");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unrecognized field");
        }
    }

    /* Test precedence of "getter-as-setter" (for Lists) versus
     * field for same property.
     */
    @Test
    public void testSetterlessPrecedence() throws Exception
    {
        ObjectMapper m = jsonMapperBuilder()
                .configure(MapperFeature.USE_GETTERS_AS_SETTERS, true)
                .build();
        Dual value = m.readValue("{\"list\":[1,2,3]}", Dual.class);
        assertNotNull(value);
        assertEquals(3, value.values.size());
    }
}

package com.fasterxml.jackson.databind.introspect;

import java.util.Map;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.POJOPropertiesCollector;
import com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder;

public class TestBuilderMethods extends BaseMapTest
{
    static class SimpleBuilder
    {
        public int x;

        @SuppressWarnings("hiding")
        public SimpleBuilder withX(int x) {
    		    this.x = x;
    		    return this;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper mapper = new ObjectMapper();
    
    public void testSimple()
    {
        POJOPropertiesCollector coll = collector(SimpleBuilder.class, "with");
        Map<String, POJOPropertyBuilder> props = coll.getPropertyMap();
        assertEquals(1, props.size());
        POJOPropertyBuilder prop = props.get("x");
        assertNotNull(prop);
        assertTrue(prop.hasField());
        assertFalse(prop.hasGetter());
        assertTrue(prop.hasSetter());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected POJOPropertiesCollector collector(Class<?> cls, String prefix)
    {
        BasicClassIntrospector bci = new BasicClassIntrospector();
        // no real difference between serialization, deserialization, at least here
        return bci.collectProperties(mapper.getSerializationConfig(),
                mapper.constructType(cls), null, false, prefix);
    }
    
}

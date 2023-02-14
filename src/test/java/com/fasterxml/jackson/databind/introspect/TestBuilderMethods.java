package com.fasterxml.jackson.databind.introspect;

import java.util.Map;

import com.fasterxml.jackson.databind.*;

public class TestBuilderMethods extends BaseMapTest
{
    static class SimpleBuilder
    {
        public int x;

        public SimpleBuilder withX(int x0) {
    		    this.x = x0;
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
        POJOPropertiesCollector coll = collector(SimpleBuilder.class);
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

    protected POJOPropertiesCollector collector(Class<?> cls)
    {
        BasicClassIntrospector bci = new BasicClassIntrospector();
        // no real difference between serialization, deserialization, at least here
        return bci.collectPropertiesWithBuilder(mapper.getSerializationConfig(),
                mapper.constructType(cls), null, null, false);
    }
}

package com.fasterxml.jackson.databind.creators;

import java.beans.ConstructorProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

public class CreatorPropertiesTest extends BaseMapTest
{
    static class Issue905Bean {
        // 08-Nov-2015, tatu: Note that in real code we would most likely use same
        //    names for properties; but here we use different name on purpose to
        //    ensure that Jackson has no way of binding JSON properties "x" and "y"
        //    using any other mechanism than via `@ConstructorProperties` annotation
        public int _x, _y;

        @ConstructorProperties({"x", "y"})
        // Same as above; use differing local parameter names so that parameter name
        // introspection can not be used as the source of property names.
        public Issue905Bean(int a, int b) {
            _x = a;
            _y = b;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#905]
    public void testCreatorPropertiesAnnotation() throws Exception
    {
        Issue905Bean b = MAPPER.readValue(aposToQuotes("{'y':3,'x':2}"),
                Issue905Bean.class);
        assertEquals(2, b._x);
        assertEquals(3, b._y);
    }
}

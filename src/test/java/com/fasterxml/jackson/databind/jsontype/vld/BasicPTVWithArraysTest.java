package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests related to [databind#2534], support for configuring
 * {@link BasicPolymorphicTypeValidator} to allow all Array-valued
 * polymorphic values.
 */
public class BasicPTVWithArraysTest extends BaseMapTest
{
    static abstract class Base2534 {
        public int x = 3;
    }

    static class Good2534 extends Base2534 {
        protected Good2534() { }
        public Good2534(int x) {
            super();
            this.x = x;
        }
    }

    static class Bad2534 extends Base2534 {
        protected Bad2534() { }
        public Bad2534(int x) {
            super();
            this.x = x;
        }
    }

    static final class ObjectWrapper {
        public Object value;

        protected ObjectWrapper() { }
        public ObjectWrapper(Object v) { value = v; }
    }

    /*
    /**********************************************************************
    /* Test methods: structured types
    /**********************************************************************
     */

    // [databind#2534]: handle Java array-types appropriately wrt validation
    public void testAllowBySubClassInArray() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Good2534.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        final String json = mapper.writeValueAsString(new ObjectWrapper(new Base2534[] { new Good2534(42) }));

        // First test blocked case:
        try {
            mapper.readValue(json, ObjectWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id '[Lcom.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }

        // and then accepted:
        ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray() // key addition
                .allowIfSubType(Good2534.class)
                .build();
        mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        ObjectWrapper w = mapper.readValue(json, ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.value);
        assertEquals(Base2534[].class, w.value.getClass());
        Base2534[] arrayOut = (Base2534[]) w.value;
        assertEquals(1, arrayOut.length);
        assertEquals(42, arrayOut[0].x);

        // but ensure array-acceptance does NOT allow non-validated element types!
        final String badJson = mapper.writeValueAsString(new ObjectWrapper(new Base2534[] { new Bad2534(13) }));
        try {
            mapper.readValue(badJson, ObjectWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "$Bad2534");
            verifyException(e, "as a subtype of");
        }
    }
}

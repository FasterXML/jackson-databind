package com.fasterxml.jackson.failing;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTV2532Test extends BaseMapTest
{
    // // // Value types

    static abstract class Base2532 {
        public int x = 3;
    }

    static class Good2532 extends Base2532 {
        protected Good2532() { }
        public Good2532(int x) {
            super();
            this.x = x;
        }
    }

    static class Bad2532 extends Base2532 {
        protected Bad2532() { }
        public Bad2532(int x) {
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

    // [databind#2532]: handle Java array-types appropriately wrt validation
    public void testAllowBySubClassInArray() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Good2532.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        final ObjectWrapper input = new ObjectWrapper(new Base2532[] { new Good2532(42) });
        final String json = mapper.writeValueAsString(input);

System.err.println("JSON: "+json);
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
                .allowIfSubType(Good2532.class)
                .build();
        mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        ObjectWrapper w = mapper.readValue(json, ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.value);
        assertEquals(Base2532[].class, w.value.getClass());
        Base2532[] arrayOut = (Base2532[]) w.value;
        assertEquals(1, arrayOut.length);

        assertEquals(42, arrayOut[0].x);
    }
}

package com.fasterxml.jackson.databind.jsontype.vld;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTVTest extends BaseMapTest
{
    // // // Value types

    static abstract class BaseValue {
        public int x = 3;
    }

    static class ValueA extends BaseValue {
        protected ValueA() { }
        public ValueA(int x) {
            super();
            this.x = x;
        }
    }

    static class ValueB extends BaseValue {
        protected ValueB() { }
        public ValueB(int x) {
            super();
            this.x = x;
        }
    }

    // // // Value types

    // make this type `final` to avoid polymorphic handling
    static final class BaseValueWrapper {
        public BaseValue value;

        protected BaseValueWrapper() { }

        public static BaseValueWrapper withA(int x) {
            BaseValueWrapper w = new BaseValueWrapper();
            w.value = new ValueA(x);
            return w;
        }

        public static BaseValueWrapper withB(int x) {
            BaseValueWrapper w = new BaseValueWrapper();
            w.value = new ValueB(x);
            return w;
        }
    }

    static final class ObjectWrapper {
        public Object value;

        protected ObjectWrapper() { }
        public ObjectWrapper(Object v) { value = v; }
    }

    static final class NumberWrapper {
        public Number value;

        protected NumberWrapper() { }
        public NumberWrapper(Number v) { value = v; }
    }
    
    /*
    /**********************************************************************
    /* Test methods: annotated
    /**********************************************************************
     */

    public void testAllowByBaseClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(BaseValue.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .enableDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        try {
            mapper.readValue(json2, NumberWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id `java.lang.Byte`");
            verifyException(e, "as a subtype of");
        }

        // and then yet again accepted one with different config
        ObjectMapper mapper2 = jsonMapperBuilder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Number.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        NumberWrapper nw = mapper2.readValue(json, NumberWrapper.class);
        assertNotNull(nw);
        assertEquals(Byte.valueOf((byte) 4), nw.value);
    }
}


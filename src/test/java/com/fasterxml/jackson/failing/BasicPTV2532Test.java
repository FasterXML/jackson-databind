package com.fasterxml.jackson.failing;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTV2523Test extends BaseMapTest
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
    /* Test methods: structured types
    /**********************************************************************
     */

    // [databind#2523]: handle Java array-types appropriately wrt validation
    public void testAllowBySubClassInArray() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
//                .allowIfSubType(BaseValue[].class)
                .allowIfSubType(BaseValue.class)
                .allowIfSubType(ValueB.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();        

        // First, test accepted case
        final ObjectWrapper input = new ObjectWrapper(new BaseValue[] { new ValueB(42) });
        final String json = mapper.writeValueAsString(input);
System.err.println("DEBUG: json == "+json);
        ObjectWrapper w = mapper.readValue(json, ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.value);
        assertEquals(BaseValue[].class, w.value.getClass());
        BaseValue[] arrayOut = (BaseValue[]) w.value;
        assertEquals(1, arrayOut.length);

        assertEquals(42, arrayOut[0].x);

        // then non-accepted
        try {
            final ObjectWrapper input2 = new ObjectWrapper(new BaseValue[] { new ValueA(43) });
            ObjectWrapper output2 = mapper.readValue(mapper.writeValueAsString(input2),
                    ObjectWrapper.class);
System.err.println("Output2 == "+output2);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }
    }
}

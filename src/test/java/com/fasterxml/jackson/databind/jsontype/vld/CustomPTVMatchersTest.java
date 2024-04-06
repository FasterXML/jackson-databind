package com.fasterxml.jackson.databind.jsontype.vld;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class CustomPTVMatchersTest extends DatabindTestUtil
{
    static abstract class CustomBase {
        public int x = 3;
    }

    static class CustomGood extends CustomBase {
        protected CustomGood() { }
        public CustomGood(int x) {
            super();
            this.x = x;
        }
    }

    static class CustomBad extends CustomBase {
        protected CustomBad() { }
        public CustomBad(int x) {
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
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testCustomBaseMatchers() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(new BasicPolymorphicTypeValidator.TypeMatcher() {
                    @Override
                    public boolean match(MapperConfig<?> ctxt, Class<?> base) {
                        // Allow types within our packages
                        return base.getName().startsWith("com.fasterxml.");
                    }
                })
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
        // First: in this case, allow "Bad" one too (note: default typing based on
        // runtime type here)
        final String goodJson = mapper.writeValueAsString(new CustomBad(42) );
        CustomBase result = mapper.readValue(goodJson, CustomBase.class);
        assertEquals(CustomBad.class, result.getClass());

        // but other types not so good
        final String badJson = mapper.writeValueAsString(TimeZone.getDefault());
        try {
            mapper.readValue(badJson, TimeZone.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'java.util.TimeZone'");
            verifyException(e, "as a subtype of");
        }
        assertEquals(CustomBad.class, result.getClass());
    }

    @Test
    public void testCustomSubtypeMatchers() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(new BasicPolymorphicTypeValidator.TypeMatcher() {
                    @Override
                    public boolean match(MapperConfig<?> ctxt, Class<?> clazz) {
                        // Allow anything that looks "Good" :)
                        return clazz.getSimpleName().endsWith("Good");
                    }
                })
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First: allow "Good" one:
        final String goodJson = mapper.writeValueAsString(new ObjectWrapper(new CustomGood(42) ));
        ObjectWrapper w = mapper.readValue(goodJson, ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.value);
        assertEquals(CustomGood.class, w.value.getClass());

        // Then something else
        final String badJson = mapper.writeValueAsString(new ObjectWrapper(new CustomBad(42) ));
        try {
            mapper.readValue(badJson, ObjectWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "Could not resolve type id 'com.fasterxml.jackson.");
            verifyException(e, "as a subtype of");
        }
    }
}

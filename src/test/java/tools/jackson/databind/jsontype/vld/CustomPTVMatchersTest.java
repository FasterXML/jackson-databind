package tools.jackson.databind.jsontype.vld;

import java.util.TimeZone;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

public class CustomPTVMatchersTest extends BaseMapTest
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

    public void testCustomBaseMatchers() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // Allow types within our packages
                .allowIfBaseType((ctxt, base) -> base.getName().startsWith("tools.jackson." ))
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
        // NOTE! Need to use non-final type (2.x used java.net.URL)
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

    public void testCustomSubtypeMatchers() throws Exception
    {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // Allow anything that looks "Good" :)
                .allowIfSubType((ctxt, sub) -> sub.getSimpleName().endsWith("Good") )
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
            verifyException(e, "Could not resolve type id 'tools.jackson.");
            verifyException(e, "as a subtype of");
        }
    }
}

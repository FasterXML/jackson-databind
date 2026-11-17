package tools.jackson.databind.jsontype.vld;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests related to [databind#2534], support for configuring
 * {@link BasicPolymorphicTypeValidator} to allow all Array-valued
 * polymorphic values.
 */
public class BasicPTVWithArraysTest extends DatabindTestUtil
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
    @Test
    public void testAllowBySubClassInArray() throws Exception {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Good2534.class)
                .build();
        final ObjectMapper mapper1 = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        final String json = mapper1.writeValueAsString(new ObjectWrapper(new Base2534[] { new Good2534(42) }));

        // First test blocked case:
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper1.readValue(json, ObjectWrapper.class));
        verifyException(e, "Could not resolve type id '[Ltools.jackson.");
        verifyException(e, "as a subtype of");

        // and then accepted:
        PolymorphicTypeValidator ptv2 = BasicPolymorphicTypeValidator.builder()
                .allowIfSubTypeIsArray() // key addition
                // [CVE-2026-110879]: abstract element type must be allowed too (but
                // only exactly it, not its subtypes)
                .allowIfSubType(Pattern.compile(Pattern.quote(Base2534.class.getName())))
                .allowIfSubType(Good2534.class)
                .build();
        final ObjectMapper mapper2 = jsonMapperBuilder()
                .activateDefaultTyping(ptv2, DefaultTyping.NON_FINAL)
                .build();

        ObjectWrapper w = mapper2.readValue(json, ObjectWrapper.class);
        assertNotNull(w);
        assertNotNull(w.value);
        assertEquals(Base2534[].class, w.value.getClass());
        Base2534[] arrayOut = (Base2534[]) w.value;
        assertEquals(1, arrayOut.length);
        assertEquals(42, arrayOut[0].x);

        // but ensure array-acceptance does NOT allow non-validated element types!
        final String badJson = mapper2.writeValueAsString(new ObjectWrapper(new Base2534[] { new Bad2534(13) }));
        InvalidTypeIdException e2 = assertThrows(InvalidTypeIdException.class,
                () -> mapper2.readValue(badJson, ObjectWrapper.class));
        verifyException(e2, "Could not resolve type id 'tools.jackson.");
        verifyException(e2, "$Bad2534");
        verifyException(e2, "as a subtype of");
    }

    // [CVE-2026-110879]: `Number` is not abstract-like wrt gadgets: accepted
    // without allow-listing (elements are scalars or have type ids of their own)
    @Test
    public void testNumberArrayAccepted() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubTypeIsArray()
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        final Number[] input = new Number[] { 1, 2.5 };
        ObjectWrapper w = mapper.readValue(mapper.writeValueAsString(new ObjectWrapper(input)),
                ObjectWrapper.class);
        assertEquals(Number[].class, w.value.getClass());
        assertArrayEquals(input, (Number[]) w.value);

        // but not other abstract element types
        final String json = mapper.writeValueAsString(new ObjectWrapper(new Base2534[0]));
        try {
            mapper.readValue(json, ObjectWrapper.class);
            fail("Should not pass");
        } catch (InvalidTypeIdException e) {
            verifyException(e, "$Base2534");
        }
    }
}

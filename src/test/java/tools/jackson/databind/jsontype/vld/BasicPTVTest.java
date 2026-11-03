package tools.jackson.databind.jsontype.vld;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the main user-configurable {@code PolymorphicTypeValidator},
 * {@link BasicPolymorphicTypeValidator}.
 */
public class BasicPTVTest extends DatabindTestUtil
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

    // [databind#2539]
    static class Dangerous2539 {
        public int x;
    }

    /*
    /**********************************************************************
    /* Test methods: by base type, pass
    /**********************************************************************
     */

    // First: test simple Base-type-as-class allowing
    @Test
    public void testAllowByBaseClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(BaseValue.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, NumberWrapper.class));
        verifyException(e, "Could not resolve type id 'java.lang.Byte'");
        verifyException(e, "as a subtype of");

        // and then yet again accepted one with different config
        ObjectMapper mapper2 = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Number.class)
                        .build(), DefaultTyping.NON_FINAL)
                .build();
        NumberWrapper nw = mapper2.readValue(json2, NumberWrapper.class);
        assertNotNull(nw);
        assertEquals(Byte.valueOf((byte) 4), nw.value);
    }

    // Then subtype-prefix
    @Test
    public void testAllowByBaseClassPrefix() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType("tools.jackson.")
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, NumberWrapper.class));
        verifyException(e, "Could not resolve type id 'java.lang.Byte'");
        verifyException(e, "as a subtype of");
    }

    // Then subtype-pattern
    @Test
    public void testAllowByBaseClassPattern() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Pattern.compile("\\w+\\.jackson\\..+"))
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withA(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(new NumberWrapper(Byte.valueOf((byte) 4)));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, NumberWrapper.class));
        verifyException(e, "Could not resolve type id 'java.lang.Byte'");
        verifyException(e, "as a subtype of");
    }

    // And finally, block by specific direct-match base type
    @Test
    public void testDenyByBaseClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // indicate that all subtypes `BaseValue` would be fine
                .allowIfBaseType(BaseValue.class)
                // but that nominal base type MUST NOT be `Object.class`
                .denyForExactBaseType(Object.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
        final String json = mapper.writeValueAsString(new ObjectWrapper(new ValueA(15)));
        // NOTE: different exception type since denial was for whole property, not just specific values
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue(json, ObjectWrapper.class));
        verifyException(e, "denied resolution of all subtypes of base type `java.lang.Object`");
    }

    /*
    /**********************************************************************
    /* Test methods: by sub type
    /**********************************************************************
     */

    @Test
    public void testAllowBySubClass() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ValueB.class)
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(BaseValueWrapper.withA(43));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, BaseValueWrapper.class));
        verifyException(e, "Could not resolve type id 'tools.jackson.");
        verifyException(e, "as a subtype of");
    }

    @Test
    public void testAllowBySubClassPrefix() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(ValueB.class.getName())
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(BaseValueWrapper.withA(43));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, BaseValueWrapper.class));
        verifyException(e, "Could not resolve type id 'tools.jackson.");
        verifyException(e, "as a subtype of");
    }

    @Test
    public void testAllowBySubClassPattern() throws Exception {
        final PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Pattern.compile(Pattern.quote(ValueB.class.getName())))
                .build();
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();

        // First, test accepted case
        final String json = mapper.writeValueAsString(BaseValueWrapper.withB(42));
        BaseValueWrapper w = mapper.readValue(json, BaseValueWrapper.class);
        assertEquals(42, w.value.x);

        // then non-accepted
        final String json2 = mapper.writeValueAsString(BaseValueWrapper.withA(43));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> mapper.readValue(json2, BaseValueWrapper.class));
        verifyException(e, "Could not resolve type id 'tools.jackson.");
        verifyException(e, "as a subtype of");
    }

    // [databind#2539]
    @Test
    public void testWithJDKBasicsOk() throws Exception
    {
        final ObjectMapper defaultingMapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowSubTypesWithExplicitDeserializer()
                        .build(),
                        DefaultTyping.NON_FINAL_AND_ENUMS)
                .build();

        Object[] input = new Object[] {
                "test", 42, new java.net.URL("http://localhost"),
                java.util.UUID.nameUUIDFromBytes("abc".getBytes()),
                new Object[] { }
        };

        String json = defaultingMapper.writeValueAsString(input);
        Object result = defaultingMapper.readValue(json, Object.class);
        assertEquals(Object[].class, result.getClass());

        // but then non-ok case:
        final String json2 = defaultingMapper.writeValueAsString(new Object[] {
                new Dangerous2539()
        });
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> defaultingMapper.readValue(json2, Object.class));
        verifyException(e, "Could not resolve type id 'tools.jackson.");
        verifyException(e, "as a subtype of");

        // and another one within array
        final String json3 = defaultingMapper.writeValueAsString(new Object[] {
                new Dangerous2539[] { new Dangerous2539() }
        });
        InvalidTypeIdException e2 = assertThrows(InvalidTypeIdException.class,
                () -> defaultingMapper.readValue(json3, Object.class));
        verifyException(e2, "Could not resolve type id '[Ltools.jackson.");
        verifyException(e2, "as a subtype of");
    }

    // [databind#6350]: `java.sql.Date` and `java.sql.Timestamp` have deserializers
    // in databind, so must be allowed like `java.util.Date`
    @Test
    public void withJavaSqlDatesOk() throws Exception
    {
        final ObjectMapper defaultingMapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowSubTypesWithExplicitDeserializer()
                        .build(),
                        DefaultTyping.NON_FINAL)
                .build();

        for (Object value : new Object[] {
                new java.util.Date(0L), new java.sql.Date(0L), new java.sql.Timestamp(0L) }) {
            String json = defaultingMapper.writeValueAsString(new ObjectWrapper(value));
            ObjectWrapper result = defaultingMapper.readValue(json, ObjectWrapper.class);
            assertEquals(value.getClass(), result.value.getClass());
            assertEquals(value, result.value);
        }

        // but `java.sql.Time` has no explicit deserializer, so must be denied
        final String json2 = defaultingMapper.writeValueAsString(
                new ObjectWrapper(new java.sql.Time(0L)));
        InvalidTypeIdException e = assertThrows(InvalidTypeIdException.class,
                () -> defaultingMapper.readValue(json2, ObjectWrapper.class));
        verifyException(e, "Could not resolve type id 'java.sql.Time'");
        verifyException(e, "as a subtype of");
    }

    // [databind#6350]: `OptionalInt`, `OptionalLong` and `OptionalDouble` also
    // have explicit deserializers
    @Test
    public void withJdkPrimitiveOptionalsOk() throws Exception
    {
        final ObjectMapper defaultingMapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowSubTypesWithExplicitDeserializer()
                        .build(),
                        DefaultTyping.NON_FINAL)
                .build();

        for (Object value : new Object[] {
                OptionalInt.of(3), OptionalLong.of(42L), OptionalDouble.of(0.5) }) {
            String json = defaultingMapper.writeValueAsString(new ObjectWrapper(value));
            ObjectWrapper result = defaultingMapper.readValue(json, ObjectWrapper.class);
            assertEquals(value, result.value);
        }
    }

    // `JsonNode` types (and `TokenBuffer`) have explicit deserializers too
    @Test
    public void withJsonNodeOk() throws Exception
    {
        final ObjectMapper defaultingMapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowSubTypesWithExplicitDeserializer()
                        .build(),
                        DefaultTyping.NON_FINAL)
                .build();

        ObjectNode node = defaultingMapper.createObjectNode();
        node.put("x", 1);
        String json = defaultingMapper.writeValueAsString(new ObjectWrapper(node));
        ObjectWrapper result = defaultingMapper.readValue(json, ObjectWrapper.class);
        assertEquals(node, result.value);
    }
}

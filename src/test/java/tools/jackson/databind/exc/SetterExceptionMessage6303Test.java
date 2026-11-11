package tools.jackson.databind.exc;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6303]: `IllegalArgumentException` thrown by a setter should be wrapped with
// its own message, not reported as a type mismatch; actual type mismatches should be
// reported with the expected type
class SetterExceptionMessage6303Test
{
    // Throws exception indicated by value
    static class ThrowingBean {
        public void setValue(String value) {
            throw switch (value) {
                case "iae" -> new IllegalArgumentException("invalid");
                case "iae-no-msg" -> new IllegalArgumentException();
                case "ise" -> new IllegalStateException("bad state");
                case "npe" -> new NullPointerException("npe from setter");
                default -> new ClassCastException("cce from setter");
            };
        }
    }

    static class IntBean {
        public void setValue(int value) { throw new IllegalArgumentException("invalid"); }
    }

    // Property set after Creator call, from buffered value
    static class CreatorBean {
        @JsonCreator
        CreatorBean(@JsonProperty("id") String id) { }

        public void setValue(String value) { throw new IllegalArgumentException("invalid"); }
    }

    @JsonDeserialize(builder = ThrowingBuilder.class)
    static class BuiltBean { }

    @JsonPOJOBuilder
    static class ThrowingBuilder {
        public ThrowingBuilder withValue(String value) { throw new IllegalArgumentException("invalid"); }

        public BuiltBean build() { return new BuiltBean(); }
    }

    static class AnySetterBean {
        @JsonAnySetter
        public void set(String key, String value) { throw new IllegalArgumentException("invalid"); }
    }

    // Any-property set after Creator call, from buffered value
    static class CreatorAnySetterBean extends AnySetterBean {
        @JsonCreator
        CreatorAnySetterBean(@JsonProperty("id") String id) { }
    }

    // Returns `String` (or `null` for `null`) regardless of the property type
    static class StringDeserializer extends ValueDeserializer<Object> {
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString();
        }
    }

    static class IntegerSetterBean {
        @JsonDeserialize(using = StringDeserializer.class)
        public void setValue(Integer value) { }
    }

    static class IntegerFieldBean {
        @JsonDeserialize(using = StringDeserializer.class)
        public Integer value;
    }

    static class PrimitiveSetterBean {
        @JsonDeserialize(using = StringDeserializer.class)
        public void setValue(int value) { }
    }

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final ObjectMapper NO_WRAP_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.WRAP_EXCEPTIONS)
            .build();

    @Test
    void illegalArgumentFromSetter() {
        DatabindException e = _assertMessage(MAPPER, "iae", ThrowingBean.class,
                "Problem setting property 'value': invalid");
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals("value", e.getPath().get(0).getPropertyName());

        _assertMessage(MAPPER, "iae-no-msg", ThrowingBean.class,
                "Problem setting property 'value': (was java.lang.IllegalArgumentException)");
        _assertMessage(MAPPER, 1, IntBean.class,
                "Problem setting property 'value': invalid");
        // Property name must be in message as path is not added for all of these
        _assertMessage(MAPPER, "x", CreatorBean.class,
                "Problem setting property 'value': invalid");
        _assertMessage(MAPPER, "x", BuiltBean.class,
                "Problem setting property 'value': invalid");
        for (Class<?> type : new Class<?>[] { AnySetterBean.class, CreatorAnySetterBean.class }) {
            _assertMessage(MAPPER, "x", type, "Problem setting \"any-property\" 'value' of class `"
                    +AnySetterBean.class.getName()+"`: invalid");
        }
    }

    // NPE and CCE thrown by the setter itself are not type mismatches
    @Test
    void npeAndCceFromSetter() {
        _assertMessage(MAPPER, "npe", ThrowingBean.class, "npe from setter");
        _assertMessage(MAPPER, "cce", ThrowingBean.class, "cce from setter");
    }

    @Test
    void typeMismatch() {
        for (Class<?> type : new Class<?>[] { IntegerSetterBean.class, IntegerFieldBean.class }) {
            _assertMismatch("x", type, ClassCastException.class,
                    "(expected type: [simple type, class java.lang.Integer]; actual type: `java.lang.String`)");
        }
        _assertMismatch(null, PrimitiveSetterBean.class, NullPointerException.class,
                "(expected type: [simple type, class int]; actual type: [null])");
    }

    // `IllegalArgumentException` is wrapped even if `WRAP_EXCEPTIONS` is disabled
    // (as before); other exceptions, including type mismatches, are not
    @Test
    void wrappingDisabled() {
        _assertMessage(NO_WRAP_MAPPER, "iae", ThrowingBean.class,
                "Problem setting property 'value': invalid");
        assertThrows(IllegalStateException.class, () -> _read(NO_WRAP_MAPPER, "ise", ThrowingBean.class));
        assertThrows(NullPointerException.class, () -> _read(NO_WRAP_MAPPER, "npe", ThrowingBean.class));
        assertThrows(ClassCastException.class, () -> _read(NO_WRAP_MAPPER, "cce", ThrowingBean.class));
        assertThrows(ClassCastException.class, () -> _read(NO_WRAP_MAPPER, "x", IntegerSetterBean.class));
        assertThrows(ClassCastException.class, () -> _read(NO_WRAP_MAPPER, "x", IntegerFieldBean.class));
        assertThrows(NullPointerException.class, () -> _read(NO_WRAP_MAPPER, null, PrimitiveSetterBean.class));
    }

    private Object _read(ObjectMapper mapper, Object value, Class<?> type) {
        String json = (value instanceof String) ? "\"" + value + "\"" : String.valueOf(value);
        // "id" after "value" so that value is buffered for Creator-based types
        return mapper.readValue("{\"value\":" + json + ",\"id\":\"1\"}", type);
    }

    // Message ends with JDK-provided "problem: ..." so only check start
    private void _assertMismatch(Object value, Class<?> type,
            Class<? extends Throwable> causeType, String expTypes)
    {
        DatabindException e = assertThrows(DatabindException.class,
                () -> _read(MAPPER, value, type));
        String msg = e.getOriginalMessage();
        assertTrue(msg.startsWith("Problem deserializing property 'value' " + expTypes), msg);
        assertInstanceOf(causeType, e.getCause());
    }

    private DatabindException _assertMessage(ObjectMapper mapper, Object value,
            Class<?> type, String expMsg)
    {
        DatabindException e = assertThrows(DatabindException.class,
                () -> _read(mapper, value, type));
        assertEquals(expMsg, e.getOriginalMessage());
        return e;
    }
}

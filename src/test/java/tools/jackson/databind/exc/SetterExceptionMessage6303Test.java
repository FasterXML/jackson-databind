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
// its own message, not reported as a type mismatch; actual type mismatches (which with
// `MethodHandle`s surface as `ClassCastException`, or `NullPointerException` for `null`
// passed to a primitive) should be reported with the expected type
class SetterExceptionMessage6303Test
{
    static final IllegalArgumentException INVALID_ZIP = new IllegalArgumentException("invalid zip");

    static class ZipBean {
        public void setZip(String zip) { throw INVALID_ZIP; }
    }

    static class IntBean {
        public void setN(int n) { throw new IllegalArgumentException("negative"); }
    }

    static class NoMessageBean {
        public void setZip(String zip) { throw new IllegalArgumentException(); }
    }

    static class StateBean {
        public void setZip(String zip) { throw new IllegalStateException("bad state"); }
    }

    // Property set after Creator call, from buffered value
    static class CreatorBean {
        @JsonCreator
        CreatorBean(@JsonProperty("id") String id) { }

        public void setZip(String zip) { throw INVALID_ZIP; }
    }

    @JsonDeserialize(builder = ZipBuilder.class)
    static class BuiltBean { }

    @JsonPOJOBuilder
    static class ZipBuilder {
        public ZipBuilder withZip(String zip) { throw INVALID_ZIP; }

        public BuiltBean build() { return new BuiltBean(); }
    }

    static class AnySetterBean {
        @JsonAnySetter
        public void set(String key, String value) { throw INVALID_ZIP; }
    }

    // Returns `String` regardless of the property type
    static class StringDeserializer extends ValueDeserializer<Object> {
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            return p.getString();
        }
    }

    // Returns `null` (and does not override `getNullValue()`)
    static class NullDeserializer extends ValueDeserializer<Object> {
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            return null;
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
        @JsonDeserialize(using = NullDeserializer.class)
        public void setValue(int value) { }
    }

    // NPE and CCE thrown by the setter itself, for a value of the expected type
    static class NpeBean {
        public void setValue(String value) { throw new NullPointerException("npe from setter"); }
    }

    static class CceBean {
        public void setValue(Object value) { throw new ClassCastException("cce from setter"); }
    }

    private static final String ZIP_JSON = """
            {"zip":"x"}
            """;

    private static final String VALUE_JSON = """
            {"value":"x"}
            """;

    private final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final ObjectMapper NO_WRAP_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.WRAP_EXCEPTIONS)
            .build();

    @Test
    void setterIllegalArgumentKeepsOwnMessage() {
        DatabindException e = _assertOwnMessage(MAPPER, ZIP_JSON, ZipBean.class,
                "invalid zip");
        assertSame(INVALID_ZIP, e.getCause());
        assertEquals("zip", e.getPath().get(0).getPropertyName());
    }

    @Test
    void illegalArgumentKeepsOwnMessageForOtherSetterPaths() {
        _assertOwnMessage(MAPPER, """
                {"zip":"x","id":"1"}
                """, CreatorBean.class, "invalid zip");
        _assertOwnMessage(MAPPER, ZIP_JSON, BuiltBean.class, "invalid zip");
        _assertOwnMessage(MAPPER, ZIP_JSON, AnySetterBean.class, "invalid zip");
    }

    @Test
    void primitiveSetterIllegalArgumentKeepsOwnMessage() {
        _assertOwnMessage(MAPPER, """
                {"n":1}
                """, IntBean.class, "negative");
    }

    @Test
    void illegalArgumentWithoutMessage() {
        _assertOwnMessage(MAPPER, ZIP_JSON, NoMessageBean.class,
                "(was java.lang.IllegalArgumentException)");
    }

    // `IllegalArgumentException` is wrapped even if `WRAP_EXCEPTIONS` is disabled
    // (as before); other exceptions from the setter are not
    @Test
    void illegalArgumentWithWrappingDisabled() {
        DatabindException e = _assertOwnMessage(NO_WRAP_MAPPER, ZIP_JSON, ZipBean.class,
                "invalid zip");
        assertSame(INVALID_ZIP, e.getCause());
        IllegalStateException ise = assertThrows(IllegalStateException.class,
                () -> NO_WRAP_MAPPER.readValue(ZIP_JSON, StateBean.class));
        assertEquals("bad state", ise.getMessage());
    }

    @Test
    void classCastMismatchReportsTargetType() {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, NO_WRAP_MAPPER }) {
            for (Class<?> type : new Class<?>[] { IntegerSetterBean.class, IntegerFieldBean.class }) {
                DatabindException e = assertThrows(DatabindException.class,
                        () -> mapper.readValue(VALUE_JSON, type));
                String msg = e.getOriginalMessage();
                assertTrue(msg.startsWith("Problem deserializing property 'value' (expected type: "), msg);
                assertTrue(msg.contains("java.lang.Integer"), msg);
                assertTrue(msg.contains("actual type: `java.lang.String`"), msg);
                assertInstanceOf(ClassCastException.class, e.getCause());
            }
        }
    }

    @Test
    void nullForPrimitiveReportsTargetType() {
        for (ObjectMapper mapper : new ObjectMapper[] { MAPPER, NO_WRAP_MAPPER }) {
            DatabindException e = assertThrows(DatabindException.class,
                    () -> mapper.readValue("""
                            {"value":1}
                            """, PrimitiveSetterBean.class));
            String msg = e.getOriginalMessage();
            assertTrue(msg.startsWith("Problem deserializing property 'value' (expected type: "), msg);
            assertTrue(msg.contains("class int"), msg);
            assertTrue(msg.contains("actual type: [null]"), msg);
            assertInstanceOf(NullPointerException.class, e.getCause());
        }
    }

    // NPE and CCE thrown by the setter itself are not type mismatches
    @Test
    void setterNpeAndCceNotReportedAsMismatch() {
        _assertOwnMessage(MAPPER, VALUE_JSON, NpeBean.class, "npe from setter");
        _assertOwnMessage(MAPPER, VALUE_JSON, CceBean.class, "cce from setter");
        assertThrows(NullPointerException.class,
                () -> NO_WRAP_MAPPER.readValue(VALUE_JSON, NpeBean.class));
        assertThrows(ClassCastException.class,
                () -> NO_WRAP_MAPPER.readValue(VALUE_JSON, CceBean.class));
    }

    private DatabindException _assertOwnMessage(ObjectMapper mapper, String json,
            Class<?> type, String expMsg)
    {
        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.readValue(json, type));
        assertEquals(expMsg, e.getOriginalMessage());
        assertFalse(e.getMessage().contains("expected type"), e.getMessage());
        return e;
    }
}

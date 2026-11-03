package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class UnknownPropertyException6375Test
{
    static class Bean {
        public int value;
    }

    record RecordBean(int value) { }

    @JsonDeserialize(builder = Builder.class)
    static class BuiltBean { }

    @JsonPOJOBuilder
    static class Builder {
        public BuiltBean build() { return new BuiltBean(); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IgnoringBean { }

    private final IllegalStateException failure = new IllegalStateException("handler failure");

    private final DeserializationProblemHandler handler = new DeserializationProblemHandler() {
        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                ValueDeserializer<?> deser, Object beanOrClass, String propName) {
            throw failure;
        }
    };

    private final ObjectMapper mapper = JsonMapper.builder().addHandler(handler).build();

    @Test
    void wrapsVanillaBeanFailure() {
        assertWrapped(mapper.readerFor(Bean.class), "{\"unknown\":1}", Bean.class);
    }

    @Test
    void wrapsActiveViewFailure() {
        ObjectMapper withView = JsonMapper.builder().addHandler(handler)
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION).build();
        assertWrapped(withView.readerFor(Bean.class).withView(Bean.class),
                "{\"unknown\":1}", Bean.class);
    }

    @Test
    void wrapsRecordFailure() {
        assertWrapped(mapper.readerFor(RecordBean.class),
                "{\"value\":1,\"unknown\":2}", RecordBean.class);
    }

    @Test
    void wrapsBuilderFailure() {
        assertWrapped(mapper.readerFor(BuiltBean.class), "{\"unknown\":1}", Builder.class);
    }

    @Test
    void disabledWrappingPreservesOriginalException() {
        ObjectMapper unwrapped = JsonMapper.builder().addHandler(handler)
                .disable(DeserializationFeature.WRAP_EXCEPTIONS).build();
        for (Class<?> type : new Class<?>[] { Bean.class, RecordBean.class, BuiltBean.class }) {
            String json = (type == RecordBean.class)
                    ? "{\"value\":1,\"unknown\":2}" : "{\"unknown\":1}";
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> unwrapped.readValue(json, type)));
        }
    }

    @Test
    void jacksonExceptionIsNotWrappedAgain() {
        UnrecognizedPropertyException[] original = new UnrecognizedPropertyException[1];
        ObjectMapper jacksonFailure = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                            ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                        original[0] = UnrecognizedPropertyException.from(p, beanOrClass, propName, null);
                        throw original[0];
                    }
                }).build();
        UnrecognizedPropertyException exception = assertThrows(UnrecognizedPropertyException.class,
                () -> jacksonFailure.readValue("{\"unknown\":1}", Bean.class));
        assertSame(original[0], exception);
        assertEquals(1, exception.getPath().size());
    }

    @Test
    void jacksonExceptionWithoutPathGetsUnknownPropertyPath() {
        for (boolean wrap : new boolean[] { true, false }) {
            DatabindException[] original = new DatabindException[1];
            Object[] bean = new Object[1];
            ObjectMapper jacksonFailure = JsonMapper.builder()
                    .configure(DeserializationFeature.WRAP_EXCEPTIONS, wrap)
                    .addHandler(new DeserializationProblemHandler() {
                        @Override
                        public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                                ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                            bean[0] = beanOrClass;
                            original[0] = DatabindException.from(p, "handler failure");
                            throw original[0];
                        }
                    }).build();
            DatabindException exception = assertThrows(DatabindException.class,
                    () -> jacksonFailure.readValue("{\"unknown\":1}", Bean.class));
            assertSame(original[0], exception);
            assertEquals(1, exception.getPath().size());
            assertSame(bean[0], exception.getPath().get(0).from());
            assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        }
    }

    @Test
    void inputMismatchGetsUnknownPropertyPath() {
        ObjectMapper jacksonFailure = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                            ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                        return ctxt.reportInputMismatch(Bean.class, "handler failure");
                    }
                }).build();
        DatabindException exception = assertThrows(DatabindException.class,
                () -> jacksonFailure.readValue("{\"unknown\":1}", Bean.class));
        assertEquals(1, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        assertInstanceOf(Bean.class, exception.getPath().get(0).from());
    }

    @Test
    void nestedReadFailureGetsUnknownPropertyPrefix() {
        JacksonException[] original = new JacksonException[1];
        ObjectMapper jacksonFailure = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                            ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                        try {
                            ctxt.readValue(p, Bean.class);
                        } catch (JacksonException e) {
                            original[0] = e;
                            throw e;
                        }
                        return true;
                    }
                }).build();
        DatabindException exception = assertThrows(DatabindException.class,
                () -> jacksonFailure.readValue("{\"unknown\":{\"value\":\"invalid\"}}", Bean.class));
        assertSame(original[0], exception);
        assertEquals(2, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        assertEquals("value", exception.getPath().get(1).getPropertyName());
    }

    @Test
    void existingUnknownPropertyPrefixIsNotDuplicated() {
        DatabindException[] original = new DatabindException[1];
        ObjectMapper jacksonFailure = JsonMapper.builder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                            ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                        original[0] = DatabindException.from(p, "handler failure");
                        original[0].prependPath(Bean.class, "value");
                        original[0].prependPath(beanOrClass, propName);
                        throw original[0];
                    }
                }).build();
        DatabindException exception = assertThrows(DatabindException.class,
                () -> jacksonFailure.readValue("{\"unknown\":1}", Bean.class));
        assertSame(original[0], exception);
        assertEquals(2, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        assertEquals("value", exception.getPath().get(1).getPropertyName());
    }

    @Test
    void defaultUnknownPropertyHasOnePathSegment() {
        ObjectMapper strict = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        UnrecognizedPropertyException exception = assertThrows(UnrecognizedPropertyException.class,
                () -> strict.readValue("{\"unknown\":1}", Bean.class));
        assertEquals(1, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
    }

    @Test
    void ignoredUnknownPropertyDoesNotCallHandler() {
        assertNotNull(mapper.readValue("{\"unknown\":{\"nested\":1}}", IgnoringBean.class));
    }

    private void assertWrapped(ObjectReader reader, String json, Class<?> beanType) {
        DatabindException exception = assertThrows(DatabindException.class,
                () -> reader.readValue(json));
        assertSame(failure, exception.getCause());
        assertEquals(1, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        assertTrue(exception.getPath().get(0).toString().startsWith(beanType.getName()));
    }
}

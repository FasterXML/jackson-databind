package tools.jackson.databind.deser.filter;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6375]: non-Jackson exceptions from `DeserializationProblemHandler.handleUnknownProperty()`
// should be wrapped with path to the unknown property; Jackson exceptions left as-is
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

    private static final String JSON = """
            {"unknown":1}
            """;

    private static final String RECORD_JSON = """
            {"value":1,"unknown":2}
            """;

    private final IllegalStateException failure = new IllegalStateException("handler failure");

    private final ObjectMapper mapper = mapperBuilder(failure)
            .withConfigOverride(Map.Entry.class,
                    o -> o.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.POJO)))
            .build();

    @Test
    void wrapsNonJacksonFailure() {
        assertWrapped(mapper.readerFor(Bean.class), JSON, Bean.class);
        assertWrapped(mapper.readerFor(Bean.class).withView(Bean.class), JSON, Bean.class);
        assertWrapped(mapper.readerFor(RecordBean.class), RECORD_JSON, RecordBean.class);
        assertWrapped(mapper.readerFor(BuiltBean.class), JSON, Builder.class);
        // Not just beans: any deserializer calling `handleUnknownProperty()`
        assertWrapped(mapper.readerFor(new TypeReference<Map.Entry<String, String>>() { }),
                """
                {"key":"a","unknown":1}
                """, null);
    }

    @Test
    void disabledWrappingPreservesOriginalException() {
        ObjectMapper unwrapped = mapperBuilder(failure)
                .disable(DeserializationFeature.WRAP_EXCEPTIONS).build();
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> unwrapped.readValue(JSON, Bean.class)));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> unwrapped.readValue(RECORD_JSON, RecordBean.class)));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> unwrapped.readValue(JSON, BuiltBean.class)));
    }

    @Test
    void jacksonExceptionIsNotModified() {
        for (boolean wrap : new boolean[] { true, false }) {
            DatabindException original = DatabindException.from((JsonParser) null, "handler failure");
            ObjectMapper jacksonFailure = mapperBuilder(original)
                    .configure(DeserializationFeature.WRAP_EXCEPTIONS, wrap).build();
            assertSame(original, assertThrows(DatabindException.class,
                    () -> jacksonFailure.readValue(JSON, Bean.class)));
            assertEquals(0, original.getPath().size());
        }
    }

    @Test
    void defaultUnknownPropertyHasOnePathSegment() {
        ObjectMapper strict = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        UnrecognizedPropertyException exception = assertThrows(UnrecognizedPropertyException.class,
                () -> strict.readValue(JSON, Bean.class));
        assertEquals(1, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
    }

    @Test
    void ignoredUnknownPropertyDoesNotCallHandler() {
        assertNotNull(mapper.readValue("""
                {"unknown":{"nested":1}}
                """, IgnoringBean.class));
    }

    private static JsonMapper.Builder mapperBuilder(RuntimeException toThrow) {
        return JsonMapper.builder().addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                    ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                throw toThrow;
            }
        });
    }

    private void assertWrapped(ObjectReader reader, String json, Class<?> beanType) {
        DatabindException exception = assertThrows(DatabindException.class,
                () -> reader.readValue(json));
        assertSame(failure, exception.getCause());
        assertEquals(1, exception.getPath().size());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
        if (beanType != null) {
            assertTrue(exception.getPath().get(0).toString().startsWith(beanType.getName()));
        }
    }
}

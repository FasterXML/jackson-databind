package tools.jackson.databind.deser.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.core.JsonParser;
import tools.jackson.core.TokenStreamLocation;
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

    // Checked exceptions (sneaky-thrown, e.g. from Kotlin) are wrapped as well
    @Test
    void wrapsCheckedFailure() {
        IOException checked = new IOException("checked failure");
        DatabindException exception = assertThrows(DatabindException.class,
                () -> mapperBuilder(checked).build().readValue(JSON, Bean.class));
        assertSame(checked, exception.getCause());
        assertEquals("unknown", exception.getPath().get(0).getPropertyName());
    }

    @Test
    void pathIncludesOuterLevels() {
        DatabindException exception = assertThrows(DatabindException.class,
                () -> mapper.readValue("""
                        [{"unknown":1}]
                        """, new TypeReference<List<Bean>>() { }));
        assertSame(failure, exception.getCause());
        assertEquals(2, exception.getPath().size());
        assertEquals(0, exception.getPath().get(0).getIndex());
        assertEquals("unknown", exception.getPath().get(1).getPropertyName());
    }

    // Location should come from parser passed to handler: for Records, unknown
    // properties are replayed from a buffer after the closing END_OBJECT
    @Test
    void locationFromHandlerParser() {
        final String json = """
                {
                  "unknown":2,
                  "value":1
                }
                """;
        for (Class<?> type : new Class<?>[] { Bean.class, RecordBean.class }) {
            TokenStreamLocation[] handlerLoc = new TokenStreamLocation[1];
            ObjectMapper m = JsonMapper.builder().addHandler(new DeserializationProblemHandler() {
                @Override
                public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                        ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                    handlerLoc[0] = p.currentLocation();
                    throw failure;
                }
            }).build();
            DatabindException exception = assertThrows(DatabindException.class,
                    () -> m.readValue(json, type));
            assertEquals(handlerLoc[0].getLineNr(), exception.getLocation().getLineNr());
            assertEquals(handlerLoc[0].getColumnNr(), exception.getLocation().getColumnNr());
            if (type == Bean.class) {
                assertEquals(2, exception.getLocation().getLineNr());
            }
        }
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

    private static JsonMapper.Builder mapperBuilder(Exception toThrow) {
        return JsonMapper.builder().addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                    ValueDeserializer<?> deser, Object beanOrClass, String propName) {
                return sneakyThrow(toThrow);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <E extends Exception> boolean sneakyThrow(Exception e) throws E {
        throw (E) e;
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

package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.impl.ErrorThrowingDeserializer;
import tools.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Tests for {@link ErrorThrowingDeserializer}.
 */
public class ErrorThrowingDeserializerTest
{
    static class MyValue {
        public int x;
    }

    // Verify that deserialize() re-throws the stored Error
    @Test
    public void testDeserializeThrowsError() throws Exception
    {
        NoClassDefFoundError error = new NoClassDefFoundError("com.example.Missing");
        ErrorThrowingDeserializer deser = new ErrorThrowingDeserializer(error);

        // Just need a parser instance; content does not matter since
        // ErrorThrowingDeserializer immediately throws
        ObjectMapper mapper = newJsonMapper();
        try (JsonParser p = mapper.createParser("{\"x\":1}")) {
            NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class,
                    () -> deser.deserialize(p, null));
            assertSame(error, thrown);
            assertEquals("com.example.Missing", thrown.getMessage());
        }
    }

    // Integration test: register ErrorThrowingDeserializer via module and
    // verify it defers the error until actual deserialization
    @SuppressWarnings("unchecked")
    @Test
    public void testViaModuleRegistration() throws Exception
    {
        NoClassDefFoundError error = new NoClassDefFoundError("some.missing.Class");
        ErrorThrowingDeserializer deser = new ErrorThrowingDeserializer(error);

        SimpleModule module = new SimpleModule("test");
        // Need raw type cast since ErrorThrowingDeserializer extends ValueDeserializer<Object>
        module.addDeserializer(MyValue.class,
                (ValueDeserializer<MyValue>) (ValueDeserializer<?>) deser);

        ObjectMapper mapper = newJsonMapper().rebuild()
                .addModule(module)
                .build();

        NoClassDefFoundError thrown = assertThrows(NoClassDefFoundError.class,
                () -> mapper.readValue("{\"x\":1}", MyValue.class));
        assertSame(error, thrown);
    }
}

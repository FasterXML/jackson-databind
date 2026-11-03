package tools.jackson.databind.ext.jdk8;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.util.ClassUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

// [modules-base#355]: built-in JDK 8 handlers must be recognized as standard
// implementations, so that optimization modules (like Blackbird) do not revert
// optimized properties to reflection when one of these handlers is resolved.
public class Jdk8HandlersStdImplTest
    extends DatabindTestUtil
{
    @Test
    public void jdk8HandlersMarkedAsStdImpl() {
        Class<?>[] handlers = new Class<?>[] {
                Jdk8OptionalDeserializer.class,
                OptionalIntDeserializer.class,
                OptionalLongDeserializer.class,
                OptionalDoubleDeserializer.class,
                Jdk8OptionalSerializer.class,
                OptionalIntSerializer.class,
                OptionalLongSerializer.class,
                OptionalDoubleSerializer.class,
                Jdk8StreamSerializer.class,
                IntStreamSerializer.class,
                LongStreamSerializer.class,
                DoubleStreamSerializer.class,
        };
        for (Class<?> handler : handlers) {
            assertTrue(ClassUtil.isJacksonStdImpl(handler),
                    "Expected @JacksonStdImpl on "+handler.getName());
        }
    }
}

package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// Resolving a `java.lang.Class` value from JSON must not force initialization
// (running the static initializer) of the named class.
public class ClassDeserNoStaticInitTest
{
    // Separate holder so that reading the flag does NOT initialize Gadget
    static class InitFlag {
        static volatile boolean gadgetInitialized = false;
    }

    static class Gadget {
        static {
            InitFlag.gadgetInitialized = true;
        }
    }

    static class Holder {
        public Class<?> type;
    }

    // Separate holder so that reading the flag does NOT initialize Sub
    static class SubInitFlag {
        static volatile boolean subInitialized = false;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static class Base { }

    static class Sub extends Base {
        static {
            SubInitFlag.subInitialized = true;
        }
        public int value;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void classValueDoesNotTriggerStaticInitializer() throws Exception
    {
        assertFalse(InitFlag.gadgetInitialized);

        String json = "{\"type\":\"" + Gadget.class.getName() + "\"}";
        Holder h = MAPPER.readValue(json, Holder.class);

        // Class is still resolved correctly
        assertEquals(Gadget.class, h.type);
        // ...but its static initializer must not have run
        assertFalse(InitFlag.gadgetInitialized,
                "Deserializing a Class value must not run the target class static initializer");
    }

    // Counterpart invariant: when a polymorphic type id is not just resolved but
    // actually instantiated during deserialization, the JVM must (still) run the
    // subtype's static initializer -- resolving without init does not regress this.
    @Test
    public void polymorphicInstantiationTriggersStaticInitializer() throws Exception
    {
        // `Sub.class.getName()` (below) does not initialize Sub, so the flag
        // should still be clear before we deserialize an actual instance.
        assertFalse(SubInitFlag.subInitialized);

        String json = "{\"@class\":\"" + Sub.class.getName() + "\",\"value\":42}";
        Base result = MAPPER.readValue(json, Base.class);

        assertEquals(Sub.class, result.getClass());
        assertEquals(42, ((Sub) result).value);
        // Instantiating the subtype must have run its static initializer
        assertTrue(SubInitFlag.subInitialized,
                "Instantiating a polymorphic subtype must run its static initializer");
    }
}

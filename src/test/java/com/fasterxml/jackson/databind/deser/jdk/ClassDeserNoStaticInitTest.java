package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

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
}

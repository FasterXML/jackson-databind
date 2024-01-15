package tools.jackson.databind.deser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing for a NPE due to race condition
 */
public class TestConcurrency
{
    @JsonDeserialize(using=TestBeanDeserializer.class)
    static class Bean
    {
        public int value = 42;
    }

    /*
    /**********************************************************************
    /* Helper classes
    /**********************************************************************
     */

    /**
     * Dummy deserializer used for verifying that partially handled (i.e. not yet
     * resolved) deserializers are not allowed to be used.
     */
    static class TestBeanDeserializer
        extends ValueDeserializer<Bean>
    {
        protected volatile boolean resolved = false;

        @Override
        public Bean deserialize(JsonParser p, DeserializationContext ctxt)
        {
            if (!resolved) {
                ctxt.reportInputMismatch(Bean.class,
                        "Deserializer not yet completely resolved");
            }
            Bean b = new Bean();
            b.value = 13;
            return b;
        }

        @Override
        public void resolve(DeserializationContext ctxt)
        {
            try {
                Thread.sleep(100L);
            } catch (Exception e) { }
            resolved = true;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testDeserializerResolution() throws Exception
    {
        // Let's repeat couple of times, just to be sure; thread timing is not
        // exact science; plus caching plays a role too
        final String JSON = "{\"value\":42}";

        for (int i = 0; i < 5; ++i) {
            final ObjectMapper mapper = new ObjectMapper();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        /*Bean b =*/ mapper.readValue(JSON, Bean.class);
                    } catch (Exception e) { }
                }
            };
            Thread t = new Thread(r);
            t.start();
            // then let it proceed
            Thread.sleep(10L);
            // and try the same...
            Bean b = mapper.readValue(JSON, Bean.class);
            // note: funny deserializer, mangles data.. :)
            assertEquals(13, b.value);
            t.join();
        }
    }
}

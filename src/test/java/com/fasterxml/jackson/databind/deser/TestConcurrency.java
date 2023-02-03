package com.fasterxml.jackson.databind.deser;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Testing for NPE due to race condition.
 */
public class TestConcurrency extends BaseMapTest
{
    @JsonDeserialize(using=CustomBeanDeserializer.class)
    static class Bean
    {
        public int value = 42;
    }

    /*
    /**********************************************
    /* Helper classes
    /**********************************************
     */

    /**
     * Dummy deserializer used for verifying that partially handled (i.e. not yet
     * resolved) deserializers are not allowed to be used.
     */
    static class CustomBeanDeserializer
        extends JsonDeserializer<Bean>
        implements ResolvableDeserializer
    {
        protected volatile boolean resolved = false;

        @Override
        public Bean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            if (!resolved) {
                throw new IOException("Deserializer not yet completely resolved");
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
    /**********************************************
    /* Unit tests
    /**********************************************
     */

    public void testDeserializerResolution() throws Exception
    {
        /* Let's repeat couple of times, just to be sure; thread timing is not
         * exact science; plus caching plays a role too
         */
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

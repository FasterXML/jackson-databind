package com.fasterxml.jackson.databind.module;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;

public class TestDuplicateRegistration extends BaseMapTest
{
    static class MyModule extends com.fasterxml.jackson.databind.Module {
        private final AtomicInteger counter;
        private final Object id;

        public MyModule(AtomicInteger c, Object id) {
            super();
            counter = c;
            this.id = id;
        }

        @Override
        public Object getRegistrationId() {
            return id;
        }

        @Override
        public String getModuleName() {
            return "TestModule";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
            counter.addAndGet(1);
        }
    }

    public void testDuplicateRegistration() throws Exception
    {
        // by default, duplicate registration should be prevented
        AtomicInteger counter = new AtomicInteger();
        /*ObjectMapper mapper =*/ ObjectMapper.builder()
                .addModule(new MyModule(counter, "id"))
                .addModule(new MyModule(counter, "id"))
                .addModule(new MyModule(counter, "id"))
                .build();
        assertEquals(1, counter.get());

        // but may be allowed by using non-identical id
        AtomicInteger counter2 = new AtomicInteger();
        /*ObjectMapper mapper2 =*/ ObjectMapper.builder()
                .addModule(new MyModule(counter2, "id1"))
                .addModule(new MyModule(counter2, "id2"))
                .addModule(new MyModule(counter2, "id3"))
                .build();
        assertEquals(3, counter2.get());
    }
}

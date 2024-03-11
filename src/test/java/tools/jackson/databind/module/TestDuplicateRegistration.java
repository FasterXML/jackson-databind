package tools.jackson.databind.module;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDuplicateRegistration extends DatabindTestUtil
{
    static class MyModule extends JacksonModule {
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

    @Test
    public void testDuplicateRegistration() throws Exception
    {
        // by default, duplicate registration should be prevented
        AtomicInteger counter = new AtomicInteger();
        /*ObjectMapper mapper =*/ jsonMapperBuilder()
                .addModule(new MyModule(counter, "id"))
                .addModule(new MyModule(counter, "id"))
                .addModule(new MyModule(counter, "id"))
                .build();
        assertEquals(1, counter.get());

        // but may be allowed by using non-identical id
        AtomicInteger counter2 = new AtomicInteger();
        /*ObjectMapper mapper2 =*/ jsonMapperBuilder()
                .addModule(new MyModule(counter2, "id1"))
                .addModule(new MyModule(counter2, "id2"))
                .addModule(new MyModule(counter2, "id3"))
                .build();
        assertEquals(3, counter2.get());
    }
}

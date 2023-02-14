package com.fasterxml.jackson.databind.module;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;

public class TestDuplicateRegistration extends BaseMapTest
{
    static class MyModule extends com.fasterxml.jackson.databind.Module {
        public int regCount;

        public MyModule() {
            super();
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
            ++regCount;
        }
    }

    @SuppressWarnings("deprecation")
    public void testDuplicateRegistration() throws Exception
    {
        // by default, duplicate registration should be prevented
        ObjectMapper mapper = newJsonMapper();
        assertTrue(mapper.isEnabled(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS));
        MyModule module = new MyModule();
        mapper.registerModule(module);
        mapper.registerModule(module);
        mapper.registerModule(module);
        assertEquals(1, module.regCount);

        // but may be allowed by changing setting
        mapper.disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS);
        mapper.registerModule(module);
        assertEquals(2, module.regCount);

        final MyModule module2 = new MyModule();
        // and ditto for a new instance
        @SuppressWarnings("unused")
        ObjectMapper mapper2 = jsonMapperBuilder()
                .disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)
                .addModule(module2)
                .addModule(module2)
                .addModule(module2)
                .build();
        assertEquals(3, module2.regCount);
    }
}

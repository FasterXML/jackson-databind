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

    public void testDuplicateRegistration() throws Exception
    {
        // by default, duplicate registration should be prevented
        ObjectMapper mapper = new ObjectMapper();
        assertTrue(mapper.isEnabled(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS));
        MyModule module = new MyModule();
        mapper.registerModule(module);
        mapper.registerModule(module);
        mapper.registerModule(module);
        assertEquals(1, module.regCount);

        // but may be allowed by changing setting
        ObjectMapper mapper2 = ObjectMapper.builder()
                .disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)
                .build();
        MyModule module2 = new MyModule();
        mapper2.registerModule(module2);
        mapper2.registerModule(module2);
        mapper2.registerModule(module2);
        assertEquals(3, module2.regCount);
    }
}

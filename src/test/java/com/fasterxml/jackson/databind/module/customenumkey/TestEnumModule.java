

package com.fasterxml.jackson.databind.module.customenumkey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;

public class TestEnumModule extends SimpleModule {

    public TestEnumModule() {
        super(ModuleVersion.VERSION);
    }

    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(TestEnum.class, TestEnumMixin.class);
        SimpleSerializers keySerializers = new SimpleSerializers();
        keySerializers.addSerializer(new TestEnumKeySerializer());
        context.addKeySerializers(keySerializers);
    }

    public static ObjectMapper setupObjectMapper(ObjectMapper mapper) {
        final TestEnumModule module = new TestEnumModule();
        mapper.registerModule(module);
        return mapper;
    }
}

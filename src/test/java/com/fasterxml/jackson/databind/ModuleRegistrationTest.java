package com.fasterxml.jackson.databind;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

class ModuleRegistrationTest
{

    @Test
    void copyHasSameRegisteredModulesInDifferentSetInstances() {
        ObjectMapper originalMapper = newJsonMapper();
        originalMapper.registerModule(_testModuleWithId("first"));
        originalMapper.registerModule(_testModuleWithId("second"));

        ObjectMapper copyMapper = originalMapper.copy();

        assertNotSame(originalMapper, copyMapper);
        assertNotSame(originalMapper._registeredModuleTypes, copyMapper._registeredModuleTypes);
        assertEquals(originalMapper.getRegisteredModuleIds(), copyMapper.getRegisteredModuleIds());
        assertTrue(originalMapper._registeredModuleTypes.containsAll(copyMapper._registeredModuleTypes));
    }

    @SuppressWarnings("serial")
    private SimpleModule _testModuleWithId(String id) {
        return new SimpleModule() {
            @Override
            public Object getTypeId() {
                return id;
            }
        };
    }

    @Test
    void registerNullModuleFails() {
        ObjectMapper objectMapper = newJsonMapper();
        try {
            objectMapper.registerModule(null);
            fail("should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "argument \"module\" is null");
        }
    }

    @Test
    void registerModuleWithNullNameFails() {
        ObjectMapper objectMapper = newJsonMapper();
        try {
            objectMapper.registerModule(new Module() {
                @Override
                public String getModuleName() {
                    return null;
                }

                @Override
                public Version version() {
                    return Version.unknownVersion();
                }

                @Override
                public void setupModule(SetupContext context) {}
            });
            fail("should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Module without defined name");
        }
    }

    @Test
    void registerModuleWithNullVersionFails() {
        ObjectMapper objectMapper = newJsonMapper();
        try {
            objectMapper.registerModule(new Module() {
                @Override
                public String getModuleName() {
                    return "some-module-name";
                }

                @Override
                public Version version() {
                    return null;
                }

                @Override
                public void setupModule(SetupContext context) {}
            });
            fail("should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Module without defined version");
        }
    }
    
}

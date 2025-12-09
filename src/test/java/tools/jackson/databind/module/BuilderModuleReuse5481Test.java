package tools.jackson.databind.module;

import java.util.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#5481]: JsonMapper.Builder module registration bug
 * when reusing builder instance between multiple builds with different modules.
 */
@SuppressWarnings("serial")
public class BuilderModuleReuse5481Test extends DatabindTestUtil
{
    // Test modules with distinct names for easy identification
    static class ModuleA extends SimpleModule {
        public ModuleA() {
            super("ModuleA", Version.unknownVersion());
        }
    }

    static class ModuleB extends SimpleModule {
        public ModuleB() {
            super("ModuleB", Version.unknownVersion());
        }
    }

    static class ModuleC extends SimpleModule {
        public ModuleC() {
            super("ModuleC", Version.unknownVersion());
        }
    }

    /**
     * Test case demonstrating issue #5481: when reusing a JsonMapper.Builder,
     * calling modules() multiple times between builds should reflect in each
     * new mapper instance, but currently only the first set of modules is
     * registered in all instances.
     */
    @Test
    public void testBuilderReuseWithDifferentModules() {
        ModuleA moduleA = new ModuleA();
        ModuleB moduleB = new ModuleB();
        ModuleC moduleC = new ModuleC();

        // Create a builder and register first set of modules
        JsonMapper.Builder builder = JsonMapper.builder()
                .addModule(moduleA)
                .addModule(moduleB);

        // Build first mapper
        ObjectMapper mapper1 = builder.build();

        // Verify first mapper has modules A, B
        Collection<JacksonModule> modules1 = mapper1.registeredModules();
        assertEquals(List.of("ModuleA", "ModuleB"), getModuleNames(modules1));

        // Now reuse the builder and register different modules
        builder.addModule(moduleC);

        // Build second mapper
        ObjectMapper mapper2 = builder.build();

        // BUG: Second mapper should have modules A, B, C
        // but according to issue #5481, it incorrectly only has A, B
        Collection<JacksonModule> modules2 = mapper2.registeredModules();
        assertEquals(List.of("ModuleA", "ModuleB", "ModuleC"), getModuleNames(modules2));
    }

    private List<String> getModuleNames(Collection<JacksonModule> modules) {
        return modules.stream().map(JacksonModule::getModuleName).toList();
    }
}

package tools.jackson.databind.module;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6457]: Allow modules to opt out of auto-registration
public class ModuleAutoRegister6457Test extends DatabindTestUtil
{
    public static class AutoModule6457 extends JacksonModule {
        @Override
        public String getModuleName() { return "AutoModule6457"; }

        @Override
        public Version version() { return Version.unknownVersion(); }

        @Override
        public void setupModule(SetupContext context) { }
    }

    public static class NoAutoModule6457 extends AutoModule6457 {
        @Override
        public String getModuleName() { return "NoAutoModule6457"; }

        @Override
        public boolean canAutoRegister() { return false; }
    }

    // Auto-registrable module that depends on opted-out one
    public static class DependentModule6457 extends AutoModule6457 {
        @Override
        public String getModuleName() { return "DependentModule6457"; }

        @Override
        public Iterable<? extends JacksonModule> getDependencies() {
            return List.of(new NoAutoModule6457());
        }
    }

    private final static String AUTO_NAME = AutoModule6457.class.getName();
    private final static String NO_AUTO_NAME = NoAutoModule6457.class.getName();
    private final static String DEPENDENT_NAME = DependentModule6457.class.getName();

    /**
     * Class loader that (re)defines given test module classes itself, so they
     * end up in an unnamed module (ServiceLoader ignores {@code META-INF/services}
     * entries for classes in named modules, and test classes may be patched into
     * {@code tools.jackson.databind}), and exposes a service file listing them.
     */
    static class ServiceClassLoader extends ClassLoader {
        private final Set<String> _ownClasses;
        private final URL _serviceFile;

        ServiceClassLoader(ClassLoader parent, URL serviceFile, Set<String> ownClasses) {
            super(parent);
            _serviceFile = serviceFile;
            _ownClasses = ownClasses;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!_ownClasses.contains(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    String path = name.replace('.', '/') + ".class";
                    try (InputStream in = getParent().getResourceAsStream(path)) {
                        if (in == null) {
                            throw new ClassNotFoundException(name+" (no class file '"+path+"' visible)");
                        }
                        byte[] b = in.readAllBytes();
                        c = defineClass(name, b, 0, b.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
                return c;
            }
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            if (name.equals("META-INF/services/" + JacksonModule.class.getName())) {
                return Collections.enumeration(List.of(_serviceFile));
            }
            return super.getResources(name);
        }
    }

    @TempDir
    Path tempDir;

    @Test
    void autoRegistrationSkipsOptedOutModule() throws Exception
    {
        ClassLoader cl = serviceLoader("""
                %s
                %s
                """.formatted(AUTO_NAME, NO_AUTO_NAME));

        // Discovery itself finds all modules...
        List<JacksonModule> mods = MapperBuilder.findModules(cl);
        assertEquals(List.of("AutoModule6457", "NoAutoModule6457"),
                mods.stream().map(JacksonModule::getModuleName).toList());

        // ... but auto-registration skips opted-out ones
        ObjectMapper mapper = JsonMapper.builder()
                .findAndAddModules(cl)
                .build();
        assertEquals(List.of(AUTO_NAME), registrationIds(mapper));
    }

    // No-ClassLoader variant uses thread context class loader
    @Test
    void autoRegistrationSkipsOptedOutModuleDefaultLoader() throws Exception
    {
        ClassLoader cl = serviceLoader("""
                %s
                %s
                """.formatted(AUTO_NAME, NO_AUTO_NAME));
        final Thread t = Thread.currentThread();
        final ClassLoader origCL = t.getContextClassLoader();
        ObjectMapper mapper;
        try {
            t.setContextClassLoader(cl);
            mapper = JsonMapper.builder()
                    .findAndAddModules()
                    .build();
        } finally {
            t.setContextClassLoader(origCL);
        }
        assertEquals(List.of(AUTO_NAME), registrationIds(mapper));
    }

    // Main use case: explicitly registered (configured) instance must not be
    // replaced by discovered instance (same registration id)
    @Test
    void autoRegistrationKeepsExplicitInstance() throws Exception
    {
        ClassLoader cl = serviceLoader("""
                %s
                """.formatted(NO_AUTO_NAME));
        JacksonModule explicit = new NoAutoModule6457();
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(explicit)
                .findAndAddModules(cl)
                .build();
        Collection<JacksonModule> registered = mapper.registeredModules();
        assertEquals(1, registered.size());
        assertSame(explicit, registered.iterator().next());
    }

    // Opt-out does not prevent registration as dependency of another module
    @Test
    void optedOutModuleRegisteredAsDependency() throws Exception
    {
        ClassLoader cl = serviceLoader("""
                %s
                """.formatted(DEPENDENT_NAME));
        ObjectMapper mapper = JsonMapper.builder()
                .findAndAddModules(cl)
                .build();
        assertEquals(List.of(NO_AUTO_NAME, DEPENDENT_NAME), registrationIds(mapper));
    }

    @Test
    void explicitRegistrationIgnoresOptOut()
    {
        assertTrue(new AutoModule6457().canAutoRegister());
        assertFalse(new NoAutoModule6457().canAutoRegister());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new NoAutoModule6457())
                .build();
        assertEquals(List.of(NO_AUTO_NAME), registrationIds(mapper));
    }

    private ClassLoader serviceLoader(String serviceFileContents) throws IOException
    {
        Path serviceFile = Files.createTempFile(tempDir, "services", null);
        Files.writeString(serviceFile, serviceFileContents, StandardCharsets.UTF_8);
        return new ServiceClassLoader(getClass().getClassLoader(),
                serviceFile.toUri().toURL(),
                Set.of(AUTO_NAME, NO_AUTO_NAME, DEPENDENT_NAME));
    }

    private static List<Object> registrationIds(ObjectMapper mapper) {
        return mapper.registeredModules().stream()
                .map(JacksonModule::getRegistrationId)
                .toList();
    }
}

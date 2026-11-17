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

    /**
     * Class loader that (re)defines given test module classes itself, so they
     * end up in an unnamed module (ServiceLoader ignores {@code META-INF/services}
     * entries for classes in named modules, and test classes may be patched into
     * {@code tools.jackson.databind}), and exposes a service file listing them.
     */
    static class ServiceClassLoader extends ClassLoader {
        private final Set<String> _ownClasses;
        private final URL _serviceFile;

        ServiceClassLoader(ClassLoader parent, URL serviceFile, String... ownClasses) {
            super(parent);
            _serviceFile = serviceFile;
            _ownClasses = new HashSet<>(Arrays.asList(ownClasses));
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

    @Test
    void autoRegistrationSkipsOptedOutModule(@TempDir Path tempDir) throws Exception
    {
        final String autoName = AutoModule6457.class.getName();
        final String noAutoName = NoAutoModule6457.class.getName();
        Path serviceFile = tempDir.resolve("services");
        Files.writeString(serviceFile, autoName + "\n" + noAutoName + "\n",
                StandardCharsets.UTF_8);
        ClassLoader cl = new ServiceClassLoader(getClass().getClassLoader(),
                serviceFile.toUri().toURL(), autoName, noAutoName);

        // Discovery itself finds all modules...
        List<JacksonModule> mods = MapperBuilder.findModules(cl);
        assertEquals(List.of("AutoModule6457", "NoAutoModule6457"),
                mods.stream().map(JacksonModule::getModuleName).toList());

        // ... but auto-registration skips opted-out ones

        ObjectMapper mapper = JsonMapper.builder()
                .findAndAddModules(cl)
                .build();
        assertEquals(List.of(autoName),
                mapper.registeredModules().stream()
                    .map(m -> m.getRegistrationId())
                    .toList());
    }

    @Test
    void explicitRegistrationIgnoresOptOut()
    {
        assertTrue(new AutoModule6457().canAutoRegister());
        assertFalse(new NoAutoModule6457().canAutoRegister());

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new NoAutoModule6457())
                .build();
        assertEquals(List.of(NoAutoModule6457.class.getName()),
                mapper.registeredModules().stream()
                    .map(m -> m.getRegistrationId())
                    .toList());
    }
}

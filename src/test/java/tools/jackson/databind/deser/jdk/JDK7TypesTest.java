package tools.jackson.databind.deser.jdk;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.jdk.JDKFromStringDeserializer.NioPathDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class JDK7TypesTest extends DatabindTestUtil
{
    private boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    @Test
    public void testPathRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path input = Paths.get(isWindows() ? "c:/tmp" : "/tmp", "foo.txt");
        String json = mapper.writeValueAsString(input);
        assertNotNull(json);

        Path p = mapper.readValue(json, Path.class);
        assertNotNull(p);

        assertEquals(input.toUri(), p.toUri());
        assertEquals(input.toAbsolutePath(), p.toAbsolutePath());
    }

    // [databind#6129]: Only accept "file" scheme or no scheme; reject jar:, http:, s3:, etc.
    @Test
    public void testRejectNonFileSchemes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // Bare path (no scheme) should work
        assertNotNull(mapper.readValue(q(isWindows() ? "c:/tmp/foo.txt" : "/tmp/foo.txt"),
                Path.class));

        // but non-"file" schemes should not
        _verifyRejectScheme(mapper, "jar:http://example.com/foo.jar!/path");
        _verifyRejectScheme(mapper, "http://example.com/path");
        _verifyRejectScheme(mapper, "s3://bucket/key");
        _verifyRejectScheme(mapper, "custom://something");

        // and message should list schemes that are allowed
        try {
            mapper.readValue(q("s3://bucket/key"), Path.class);
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "scheme 's3' not allowed");
            verifyException(e, "allowed: [\"file\"]");
        }
    }

    // [databind#6129]: scheme matching case-insensitive, as by `Paths.get(URI)` itself
    @Test
    public void testAllowedSchemeCaseInsensitive() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final Path input = Paths.get(isWindows() ? "c:/tmp" : "/tmp", "foo.txt");
        // Construct "file:" URI the portable way, then vary case of the scheme
        final String uriStr = input.toUri().toString();

        for (String scheme : new String[] { "FILE", "FiLe" }) {
            String json = q(scheme + uriStr.substring(4));
            Path p = mapper.readValue(json, Path.class);
            assertNotNull(p);
            assertEquals(input.toAbsolutePath(), p.toAbsolutePath());
        }
    }

    // [databind#6129]: allowed schemes may be overridden
    @Test
    public void testCustomAllowedSchemes() throws Exception
    {
        final Path input = Paths.get(isWindows() ? "c:/tmp" : "/tmp", "foo.txt");
        final String fileUri = input.toUri().toString();

        // Case-insensitive both ways: allow-list entry in upper case, input in lower
        ObjectMapper mapper = _mapperWithSchemes(Arrays.asList("FILE"));
        Path p = mapper.readValue(q(fileUri), Path.class);
        assertNotNull(p);
        assertEquals(input.toAbsolutePath(), p.toAbsolutePath());

        // But if "file" not included, it gets rejected like any other scheme
        mapper = _mapperWithSchemes(Arrays.asList("jar", "jrt"));
        try {
            mapper.readValue(q(fileUri), Path.class);
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "scheme 'file' not allowed");
            verifyException(e, "allowed: [\"jar\", \"jrt\"]");
        }

        // Scheme-less values, however, are always accepted
        assertNotNull(mapper.readValue(q(isWindows() ? "c:/tmp/foo.txt" : "/tmp/foo.txt"),
                Path.class));

        // and empty allow-list is legal, if unhelpful
        _verifyRejectScheme(_mapperWithSchemes(Collections.emptyList()), fileUri);
    }

    // [databind#6129]: allowed scheme with no provider installed must go through
    // provider look up (see [databind#2120]) and fail with the original cause --
    // and NOT with `ServiceConfigurationError` (see `module-info.java` `uses`)
    @Test
    public void testAllowedSchemeWithNoProvider() throws Exception
    {
        ObjectMapper mapper = _mapperWithSchemes(Arrays.asList("jimfs"));
        try {
            mapper.readValue(q("jimfs://bucket/foo.txt"), Path.class);
            fail("Should not pass");
        } catch (DatabindException e) {
            verifyException(e, "Provider \"jimfs\" not installed");
        }
    }

    @Test
    public void testNullAllowedSchemes() throws Exception
    {
        try {
            new NioPathDeserializer(null);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "`allowedSchemes` must not be null");
        }
    }

    // NOTE: cast needed since `JDKFromStringDeserializer` is typed as
    // `ValueDeserializer<Object>`, not `ValueDeserializer<Path>`
    @SuppressWarnings("unchecked")
    private ObjectMapper _mapperWithSchemes(Collection<String> allowedSchemes) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Path.class,
                (ValueDeserializer<Path>) (ValueDeserializer<?>) new NioPathDeserializer(allowedSchemes));
        return jsonMapperBuilder().addModule(module).build();
    }

    private void _verifyRejectScheme(ObjectMapper mapper, String input) {
        try {
            mapper.readValue(q(input), Path.class);
            fail("Should have thrown for scheme in: " + input);
        } catch (Exception e) {
            // expected - handleWeirdStringValue will throw by default
            verifyException(e, "not allowed for Path deserialization");
        }
    }

    // [databind#1688]
    @Test
    public void testPolymorphicPath() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    DefaultTyping.NON_FINAL)
            .build();
        Path input = Paths.get(isWindows() ? "c:/tmp" : "/tmp", "foo.txt");

        String json = mapper.writeValueAsString(new Object[]{input});

        Object[] obs = mapper.readValue(json, Object[].class);
        assertEquals(1, obs.length);
        Object ob = obs[0];
        if (!(ob instanceof Path)) {
            fail("Should deserialize as `Path`, got: `" + ob.getClass().getName() + "`");
        }

        assertEquals(input.toAbsolutePath().toString(), ob.toString());
    }
}

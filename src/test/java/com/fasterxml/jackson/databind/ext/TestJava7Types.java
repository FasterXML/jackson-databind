package com.fasterxml.jackson.databind.ext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class TestJava7Types extends DatabindTestUtil
{
    @Test
    public void testPathRoundtrip() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        Path input = Paths.get("/tmp", "foo.txt");

        String json = mapper.writeValueAsString(input);
        assertNotNull(json);

        Path p = mapper.readValue(json, Path.class);
        assertNotNull(p);

        assertEquals(input.toUri(), p.toUri());
        assertEquals(input.toAbsolutePath(), p.toAbsolutePath());
    }

    // [databind#]: Only accept "file" scheme or no scheme; reject jar:, http:, s3:, etc.
    @Test
    public void testRejectNonFileSchemes() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // file: scheme should work
        Path p = mapper.readValue("\"file:///tmp/foo.txt\"", Path.class);
        assertNotNull(p);

        // Bare path (no scheme) should work
        p = mapper.readValue("\"/tmp/foo.txt\"", Path.class);
        assertNotNull(p);

        // Non-file schemes should fail
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

    // [databind#]: scheme matching case-insensitive, as by `Paths.get(URI)` itself
    @Test
    public void testAllowedSchemeCaseInsensitive() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        for (String input : new String[] {
                "FILE:///tmp/foo.txt", "FiLe:///tmp/foo.txt"
        }) {
            Path p = mapper.readValue(q(input), Path.class);
            assertNotNull(p);
            assertEquals(Paths.get("/tmp", "foo.txt").toAbsolutePath(), p.toAbsolutePath());
        }
    }

    // [databind#]: allowed schemes may be overridden
    @Test
    public void testCustomAllowedSchemes() throws Exception
    {
        // Case-insensitive both ways: allow-list entry in upper case, input in lower
        ObjectMapper mapper = _mapperWithSchemes(Arrays.asList("FILE"));
        Path p = mapper.readValue(q("file:///tmp/foo.txt"), Path.class);
        assertNotNull(p);
        assertEquals(Paths.get("/tmp", "foo.txt").toAbsolutePath(), p.toAbsolutePath());

        // But if "file" not included, it gets rejected like any other scheme
        mapper = _mapperWithSchemes(Arrays.asList("jar", "jrt"));
        try {
            mapper.readValue(q("file:///tmp/foo.txt"), Path.class);
            fail("Should have thrown for scheme in: file:///tmp/foo.txt");
        } catch (Exception e) {
            verifyException(e, "scheme 'file' not allowed");
            verifyException(e, "allowed: [\"jar\", \"jrt\"]");
        }

        // Scheme-less values, however, are always accepted
        assertNotNull(mapper.readValue(q("/tmp/foo.txt"), Path.class));

        // and empty allow-list is legal, if unhelpful
        _verifyRejectScheme(_mapperWithSchemes(Collections.<String>emptyList()),
                "file:///tmp/foo.txt");
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

    private ObjectMapper _mapperWithSchemes(Collection<String> allowedSchemes) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Path.class, new NioPathDeserializer(allowedSchemes));
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

    // [databind#1688]:
    @Test
    public void testPolymorphicPath() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        DefaultTyping.NON_FINAL)
                .build();
        Path input = Paths.get("/tmp", "foo.txt");

        String json = mapper.writeValueAsString(new Object[] { input });

        Object[] obs = mapper.readValue(json, Object[].class);
        assertEquals(1, obs.length);
        Object ob = obs[0];
        assertTrue(ob instanceof Path);

        assertEquals(input.toAbsolutePath().toString(), ob.toString());
    }
}

package com.fasterxml.jackson.databind.ext;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

public class TestJava7Types extends BaseMapTest
{
    private boolean isWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

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

    // [databind#1688]:
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

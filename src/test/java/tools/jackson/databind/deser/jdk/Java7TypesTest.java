package tools.jackson.databind.deser.jdk;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

public class Java7TypesTest extends DatabindTestUtil
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

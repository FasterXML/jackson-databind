package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#3030]: Forward references with {@code @JsonIdentityInfo}
 * should work when using {@code @JsonCreator}.
 */
public class ObjectIdWithCreatorForwardRef3030Test extends DatabindTestUtil
{
    // Container with lists of B and C1 (C1 uses @JsonCreator)
    static class A {
        public List<B> bs;
        public List<C> cs;
    }

    // Class with object identity
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class B {
        public String id;
    }

    // Class that references B via @JsonCreator (problematic case)
    static class C {
        private B b;

        @JsonCreator
        public C(@JsonProperty("b") B b) {
            this.b = b;
        }

        @JsonGetter("b")
        public B getB() {
            return b;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // No forward reference: Bs comes before Cs in JSON, so B objects are
    // deserialized before C needs them - this works
    @Test
    public void testNoForwardReferenceWithCreator() throws Exception
    {
        String json = "{\"bs\":[{\"id\":\"b1\"},{\"id\":\"b2\"}],\"cs\":[{\"b\":\"b1\"},{\"b\":\"b2\"}]}";

        A result = MAPPER.readValue(json, A.class);

        assertNotNull(result);
        assertEquals(2, result.bs.size());
        assertEquals(2, result.cs.size());
        assertEquals("b1", result.bs.get(0).id);
        assertEquals("b2", result.bs.get(1).id);
        // Verify that cs reference the same B instances
        assertSame(result.bs.get(0), result.cs.get(0).getB());
        assertSame(result.bs.get(1), result.cs.get(1).getB());
    }

    // [databind#3030] Forward reference WITH @JsonCreator: cs comes before bs
    // in JSON, and C uses @JsonCreator
    @Test
    public void testForwardReferenceWithCreator() throws Exception
    {
        String json = "{\"cs\":[{\"b\":\"b1\"},{\"b\":\"b2\"}],\"bs\":[{\"id\":\"b1\"},{\"id\":\"b2\"}]}";

        A result = MAPPER.readValue(json, A.class);

        assertNotNull(result);
        assertEquals(2, result.bs.size());
        assertEquals(2, result.cs.size());
        // Verify that cs reference the same B instances
        assertSame(result.bs.get(0), result.cs.get(0).getB());
        assertSame(result.bs.get(1), result.cs.get(1).getB());
    }
}

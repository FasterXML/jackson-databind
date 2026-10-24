package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6324]: ArrayIndexOutOfBoundsException when property with both
// @JsonIdentityInfo and @JsonIgnoreProperties has value type using @JsonAlias
public class ObjectIdWithAliases6324Test extends DatabindTestUtil
{
    static class Foo {
        @JsonAlias("a1")
        public int x;

        public String name;

        public String ignored;
    }

    static class Container {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        @JsonIgnoreProperties({"ignored"})
        public Foo foo;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Reported reproduction: deserializer construction must not fail
    @Test
    public void aliasTypeWithObjectIdAndIgnoredProperties() throws Exception
    {
        Container result = MAPPER.readValue("""
                {"foo":{"x":3}}
                """, Container.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals(3, result.foo.x);
    }

    // Actual alias name "a1" must be matched, not just the primary name
    @Test
    public void aliasUsedWithObjectIdAndIgnoredProperties() throws Exception
    {
        Container result = MAPPER.readValue("""
                {"foo":{"a1":7}}
                """, Container.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals(7, result.foo.x);
    }

    // Ignored property must really be skipped while non-ignored ones are bound
    @Test
    public void ignoredSkippedWhileOtherPropertiesRead() throws Exception
    {
        Container result = MAPPER.readValue(
                """
                {"foo":{"a1":3,"name":"abc","ignored":"nope"}}
                """, Container.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals(3, result.foo.x);
        assertEquals("abc", result.foo.name);
        assertNull(result.foo.ignored);
    }

    // Object Id handling itself must keep working: id written on ser is
    // accepted (and consumed) on deser
    @Test
    public void objectIdRoundTripWithAliases() throws Exception
    {
        Container input = new Container();
        input.foo = new Foo();
        input.foo.x = 42;
        input.foo.name = "bar";

        String json = MAPPER.writeValueAsString(input);
        assertTrue(json.contains("\"@id\""), "Expected Object Id in: " + json);

        Container result = MAPPER.readValue(json, Container.class);
        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals(42, result.foo.x);
        assertEquals("bar", result.foo.name);
    }

    // Id given explicitly in input must be accepted, aliased property still bound
    @Test
    public void explicitObjectIdWithAlias() throws Exception
    {
        Container result = MAPPER.readValue(
                """
                {"foo":{"@id":1,"a1":11,"name":"qux"}}
                """, Container.class);

        assertNotNull(result);
        assertNotNull(result.foo);
        assertEquals(11, result.foo.x);
        assertEquals("qux", result.foo.name);
    }
}

package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6352]: property-level `@JsonIdentityInfo` (with `PropertyGenerator`)
// must not leak Object Id handling into other uses of the value type
public class ObjectIdPropertyLevelLeak6352Test extends DatabindTestUtil
{
    static class Foo {
        public int id;
        public String name;
    }

    static class Holder {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public Foo foo;

        public List<Foo> plain;
    }

    private static final String HOLDER_JSON = """
            {"foo":{"id":1,"name":"a"},
             "plain":[{"id":7,"name":"b"},{"id":7,"name":"c"}]}
            """;

    private static final String FOO_ARRAY_JSON = """
            [{"id":7,"name":"b"},{"id":7,"name":"c"}]
            """;

    @Test
    void holderFirst() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        _verifyHolder(mapper.readValue(HOLDER_JSON, Holder.class));
        _verifyFoos(mapper.readValue(FOO_ARRAY_JSON, Foo[].class));
    }

    @Test
    void plainFirst() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        _verifyFoos(mapper.readValue(FOO_ARRAY_JSON, Foo[].class));
        _verifyHolder(mapper.readValue(HOLDER_JSON, Holder.class));
        _verifyFoos(mapper.readValue(FOO_ARRAY_JSON, Foo[].class));
    }

    private void _verifyHolder(Holder h) {
        assertNotNull(h.foo);
        assertEquals(1, h.foo.id);
        assertEquals("a", h.foo.name);
        assertEquals(2, h.plain.size());
        _verifyFoos(h.plain.toArray(new Foo[0]));
    }

    private void _verifyFoos(Foo[] foos) {
        assertEquals(2, foos.length);
        assertEquals(7, foos[0].id);
        assertEquals("b", foos[0].name);
        assertEquals(7, foos[1].id);
        assertEquals("c", foos[1].name);
    }
}

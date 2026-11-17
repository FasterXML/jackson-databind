package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
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

        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public Foo fooRef;

        public List<Foo> plain;
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class ArrayFoo {
        public int id;
        public String name;
    }

    static class ArrayHolder {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public ArrayFoo foo;

        public List<ArrayFoo> plain;
    }

    @JsonDeserialize(builder = BuiltFoo.Builder.class)
    static class BuiltFoo {
        final int id;
        final String name;

        BuiltFoo(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            int id;
            String name;

            public Builder id(int id) { this.id = id; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public BuiltFoo build() { return new BuiltFoo(id, name); }
        }
    }

    static class BuiltHolder {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public BuiltFoo foo;

        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public BuiltFoo fooRef;

        public List<BuiltFoo> plain;
    }

    // Self-reference: contextualization happens while `Node` deserializer is
    // still being resolved (used to fail with `NoSuchElementException`)
    static class Node {
        public int id;
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public Node next;
        public String name;
    }

    private static final String HOLDER_JSON = """
            {"foo":{"id":1,"name":"a"},
             "fooRef":1,
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

    @Test
    void asArrayValue() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        ArrayHolder h = mapper.readValue("""
                {"foo":[1,"a"],
                 "plain":[[7,"b"],[7,"c"]]}
                """, ArrayHolder.class);
        assertEquals(1, h.foo.id);
        assertEquals("a", h.foo.name);
        assertEquals(2, h.plain.size());
        assertEquals("b", h.plain.get(0).name);
        assertEquals("c", h.plain.get(1).name);

        ArrayFoo[] foos = mapper.readValue("""
                [[7,"b"],[7,"c"]]
                """, ArrayFoo[].class);
        assertEquals(2, foos.length);
        assertEquals("b", foos[0].name);
        assertEquals("c", foos[1].name);
    }

    @Test
    void builderValue() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        BuiltHolder h = mapper.readValue("""
                {"foo":{"id":1,"name":"a"},
                 "fooRef":1,
                 "plain":[{"id":7,"name":"b"},{"id":7,"name":"c"}]}
                """, BuiltHolder.class);
        assertEquals(1, h.foo.id);
        assertEquals("a", h.foo.name);
        assertSame(h.foo, h.fooRef);
        assertEquals(2, h.plain.size());
        assertEquals("b", h.plain.get(0).name);
        assertEquals("c", h.plain.get(1).name);

        BuiltFoo[] foos = mapper.readValue(FOO_ARRAY_JSON, BuiltFoo[].class);
        assertEquals(2, foos.length);
        assertEquals("b", foos[0].name);
        assertEquals("c", foos[1].name);
    }

    @Test
    void selfReference() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();
        Node n = mapper.readValue("""
                {"id":1,"next":{"id":2,"name":"b","next":2},"name":"a"}
                """, Node.class);
        assertEquals("a", n.name);
        assertEquals(2, n.next.id);
        assertEquals("b", n.next.name);
        assertSame(n.next, n.next.next);
    }

    private void _verifyHolder(Holder h) {
        assertNotNull(h.foo);
        assertEquals(1, h.foo.id);
        assertEquals("a", h.foo.name);
        // Object Id handling must still work for annotated properties
        assertSame(h.foo, h.fooRef);
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

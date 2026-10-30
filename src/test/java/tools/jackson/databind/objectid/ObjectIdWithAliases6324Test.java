package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#6324]: ArrayIndexOutOfBoundsException when property with both
// @JsonIdentityInfo and @JsonIgnoreProperties has value type using @JsonAlias
public class ObjectIdWithAliases6324Test extends DatabindTestUtil
{
    static class Foo {
        @JsonAlias("a1")
        public int x;

        public String ignored;
    }

    static class Container {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        @JsonIgnoreProperties({"ignored"})
        public Foo foo;
    }

    @JsonDeserialize(builder = BuiltFoo.Builder.class)
    static class BuiltFoo {
        final int x;

        BuiltFoo(int x) {
            this.x = x;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            int x;

            @JsonAlias("a1")
            public Builder x(int x) {
                this.x = x;
                return this;
            }

            public Builder ignored(String ignored) {
                throw new IllegalStateException("Should be ignored");
            }

            public BuiltFoo build() {
                return new BuiltFoo(x);
            }
        }
    }

    static class BuiltContainer {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        @JsonIgnoreProperties({"ignored"})
        public BuiltFoo foo;
    }

    static class CreatorFoo {
        final int x;

        public String ignored;

        @JsonCreator
        CreatorFoo(@JsonProperty("x") @JsonAlias("a1") int x) {
            this.x = x;
        }
    }

    static class CreatorContainer {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        @JsonIgnoreProperties({"ignored"})
        public CreatorFoo foo;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void aliasWithObjectIdAndIgnoredProperty() throws Exception
    {
        Container result = MAPPER.readValue("""
                {"foo":{"@id":1,"a1":3,"ignored":"nope"}}
                """, Container.class);

        assertEquals(3, result.foo.x);
        assertNull(result.foo.ignored);
    }

    @Test
    public void objectIdRoundTrip() throws Exception
    {
        Container input = new Container();
        input.foo = new Foo();
        input.foo.x = 42;

        String json = MAPPER.writeValueAsString(input);
        assertEquals("""
                {"foo":{"@id":1,"x":42}}""", json);
        assertEquals(42, MAPPER.readValue(json, Container.class).foo.x);
    }

    @Test
    public void builderWithAliasObjectIdAndIgnoredProperty() throws Exception
    {
        BuiltContainer result = MAPPER.readValue("""
                {"foo":{"@id":1,"a1":5,"ignored":"nope"}}
                """, BuiltContainer.class);

        assertEquals(5, result.foo.x);
    }

    @Test
    public void creatorAliasWithObjectIdAndIgnoredProperty() throws Exception
    {
        CreatorContainer result = MAPPER.readValue("""
                {"foo":{"@id":1,"a1":9,"ignored":"nope"}}
                """, CreatorContainer.class);

        assertEquals(9, result.foo.x);
        assertNull(result.foo.ignored);
    }
}

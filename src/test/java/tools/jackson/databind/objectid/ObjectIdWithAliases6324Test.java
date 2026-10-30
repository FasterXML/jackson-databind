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
    }

    static class Container {
        @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
        @JsonIgnoreProperties({"ignored"})
        public Foo foo;
    }

    @JsonDeserialize(builder = BuiltFoo.Builder.class)
    static class BuiltFoo {
        int x;

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            final BuiltFoo foo = new BuiltFoo();

            @JsonAlias("a1")
            public Builder x(int x) {
                foo.x = x;
                return this;
            }

            public BuiltFoo build() {
                return foo;
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

    private static final String JSON = """
            {"foo":{"@id":1,"a1":3,"ignored":"nope"}}
            """;

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void beanWithAlias() throws Exception
    {
        assertEquals(3, MAPPER.readValue(JSON, Container.class).foo.x);
    }

    @Test
    public void builderWithAlias() throws Exception
    {
        assertEquals(3, MAPPER.readValue(JSON, BuiltContainer.class).foo.x);
    }

    @Test
    public void creatorWithAlias() throws Exception
    {
        assertEquals(3, MAPPER.readValue(JSON, CreatorContainer.class).foo.x);
    }
}

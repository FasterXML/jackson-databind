package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for [databind#6145]: {@code @JsonIgnoreProperties} must be honored for
 * Creator properties on the remaining property-based-Creator deserialization
 * paths too, that is external type id and the Builder-based deserializer.
 */
public class IgnorePropertiesCreator6145Test extends DatabindTestUtil
{
    /*
    /**********************************************************************
    /* Set up: external type id + property-based Creator
    /**********************************************************************
     */

    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal { }

    static class ExtTypeValue {
        public final String secret;
        public final Animal value;

        @JsonCreator
        public ExtTypeValue(@JsonProperty("secret") String secret,
                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                        include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
                @JsonSubTypes({ @JsonSubTypes.Type(value = Dog.class, name = "dog") })
                @JsonProperty("value") Animal value) {
            this.secret = secret;
            this.value = value;
        }
    }

    static class ExtTypeWrapper {
        @JsonIgnoreProperties("secret")
        public ExtTypeValue child;
    }

    /*
    /**********************************************************************
    /* Set up: Builder-based deserializer + property-based Creator
    /**********************************************************************
     */

    @JsonDeserialize(builder = BuiltValue.Builder.class)
    static class BuiltValue {
        public final int id;
        public final String secret;

        BuiltValue(int id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            private final int id;
            private final String secret;

            @JsonCreator
            public Builder(@JsonProperty("id") int id,
                    @JsonProperty("secret") String secret) {
                this.id = id;
                this.secret = secret;
            }

            public BuiltValue build() { return new BuiltValue(id, secret); }
        }
    }

    static class BuilderWrapper {
        @JsonIgnoreProperties("secret")
        public BuiltValue child;
    }

    @JsonDeserialize(builder = UnwrappedBuiltValue.Builder.class)
    static class UnwrappedBuiltValue {
        public final String secret;
        public final Name name;

        UnwrappedBuiltValue(String secret, Name name) {
            this.secret = secret;
            this.name = name;
        }

        static class Name {
            public String first;
            public String last;
        }

        @JsonPOJOBuilder(withPrefix = "")
        static class Builder {
            private final String secret;
            @JsonUnwrapped
            public Name name;

            @JsonCreator
            public Builder(@JsonProperty("secret") String secret) {
                this.secret = secret;
            }

            public UnwrappedBuiltValue build() {
                return new UnwrappedBuiltValue(secret, name);
            }
        }
    }

    static class UnwrappedBuilderWrapper {
        @JsonIgnoreProperties("secret")
        public UnwrappedBuiltValue child;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void ignoralWithExternalTypeIdCreator() throws Exception
    {
        ExtTypeWrapper result = MAPPER.readValue(
                """
                {"child":{"secret":"leaked","type":"dog","value":{"name":"Rex"}}}
                """,
                ExtTypeWrapper.class);
        assertEquals("Rex", result.child.value.name);
        assertNull(result.child.secret);
    }

    @Test
    void ignoralWithBuilderCreator() throws Exception
    {
        BuilderWrapper result = MAPPER.readValue(
                """
                {"child":{"id":13,"secret":"leaked"}}
                """,
                BuilderWrapper.class);
        assertEquals(13, result.child.id);
        assertNull(result.child.secret);
    }

    @Test
    void ignoralWithBuilderCreatorAndUnwrapped() throws Exception
    {
        UnwrappedBuilderWrapper result = MAPPER.readValue(
                """
                {"child":{"secret":"leaked","first":"Bob","last":"Smith"}}
                """,
                UnwrappedBuilderWrapper.class);
        assertEquals("Bob", result.child.name.first);
        assertEquals("Smith", result.child.name.last);
        assertNull(result.child.secret);
    }
}

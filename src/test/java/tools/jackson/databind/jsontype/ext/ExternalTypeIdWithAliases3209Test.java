package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#3209]: @JsonAlias on EXTERNAL_PROPERTY-typed value
class ExternalTypeIdWithAliases3209Test extends DatabindTestUtil {
    static class Nar {
        public String dar;
        public String zar;
    }

    static class Foo {
        public String bar;
        public String zar;
    }

    static class Container {
        public String id;

        @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "id")
        @JsonSubTypes({
                @Type(name = "FOO", value = Foo.class),
                @Type(name = "NAR", value = Nar.class)
        })
        @JsonAlias({ "fooValue", "narValue" })
        public Object target;
    }

    static class CreatorContainer {
        public final String id;
        public final Object target;

        @JsonCreator
        public CreatorContainer(
                @JsonProperty("id") String id,
                @JsonTypeInfo(use = Id.NAME, include = As.EXTERNAL_PROPERTY, property = "id")
                @JsonSubTypes({
                        @Type(name = "FOO", value = Foo.class),
                        @Type(name = "NAR", value = Nar.class)
                })
                @JsonAlias({ "fooValue", "narValue" })
                @JsonProperty("target") Object target) {
            this.id = id;
            this.target = target;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testAliasResolvesNarValue() throws Exception {
        Container c = MAPPER.readValue(
                "{ \"id\": \"NAR\", \"narValue\": { \"dar\": \"d\" }}",
                Container.class);
        assertNotNull(c.target);
        assertInstanceOf(Nar.class, c.target);
    }

    @Test
    void testAliasResolvesFooValue() throws Exception {
        Container c = MAPPER.readValue(
                "{ \"id\": \"FOO\", \"fooValue\": { \"bar\": \"b\" }}",
                Container.class);
        assertNotNull(c.target);
        assertInstanceOf(Foo.class, c.target);
    }

    @Test
    void testCanonicalNameStillWorks() throws Exception {
        Container c = MAPPER.readValue(
                "{ \"id\": \"NAR\", \"target\": { \"dar\": \"d\" }}",
                Container.class);
        assertNotNull(c.target);
        assertInstanceOf(Nar.class, c.target);
    }

    // [databind#3209]: creator-based path uses same ExternalTypeHandler,
    // so alias should also resolve when value is a @JsonCreator arg.
    @Test
    void testAliasWithCreator() throws Exception {
        CreatorContainer c = MAPPER.readValue(
                "{ \"id\": \"NAR\", \"narValue\": { \"dar\": \"d\" }}",
                CreatorContainer.class);
        assertEquals("NAR", c.id);
        assertInstanceOf(Nar.class, c.target);
    }

    @Test
    void testUnknownAliasStillFails() throws Exception {
        ObjectMapper strict = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        assertThrows(UnrecognizedPropertyException.class, () ->
                strict.readValue(
                        "{ \"id\": \"NAR\", \"bogusValue\": { \"dar\": \"d\" }}",
                        Container.class));
    }

}

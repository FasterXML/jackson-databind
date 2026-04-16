package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}

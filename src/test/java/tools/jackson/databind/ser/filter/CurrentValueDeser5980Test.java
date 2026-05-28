package tools.jackson.databind.ser.filter;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5980]: property-based-creator deserialization of an earlier property
// must not clobber the enclosing value's `JsonParser.currentValue()`
public class CurrentValueDeser5980Test extends DatabindTestUtil
{
    static class Project {
        public Source source;

        @JsonDeserialize(using = AssetDeserializer.class)
        public List<Asset> assets;
    }

    // Properties-based creator (simulates `-parameters` / Lombok @AllArgsConstructor auto-detection)
    static class Source {
        public String id;
        @JsonCreator
        public Source(@JsonProperty("id") String id) { this.id = id; }
    }

    // Properties-based creator AND has an `@JsonUnwrapped` property: exercises the
    // `deserializeUsingPropertyBasedWithUnwrapped()` path.
    static class UnwrappedSource {
        String id;
        Name name;

        @JsonCreator
        public UnwrappedSource(@JsonProperty("id") String id) { this.id = id; }

        @JsonUnwrapped
        public void setName(Name name) { this.name = name; }
    }

    static class Name {
        public String first;
        public String last;
    }

    static class ProjectWithUnwrapped {
        public UnwrappedSource source;

        @JsonDeserialize(using = AssetDeserializer.class)
        public List<Asset> assets;
    }

    static class Asset {
        public String name;
    }

    // Consume the whole array first (pops stream context back to enclosing object),
    // then look at currentValue -- as the reporter's deserializer does.
    static class AssetDeserializer extends ValueDeserializer<List<Asset>> {
        @Override
        public List<Asset> deserialize(JsonParser p, DeserializationContext ctxt) {
            p.readValueAsTree();
            Object cv = p.currentValue();
            assertNotNull(cv, "currentValue() should be the enclosing value, was null");
            // enclosing value is either Project or ProjectWithUnwrapped, never a Source
            assertFalse(cv instanceof Source || cv instanceof UnwrappedSource,
                    "currentValue() should be the enclosing value, was: " + cv.getClass().getName());
            return List.of();
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // Property-based creator sibling must not clobber enclosing currentValue
    @Test
    public void currentValueNotClobberedByCreator() throws Exception {
        String json = """
                { "source": { "id": "s1" }, "assets": [ { "name": "a1" } ] }
                """;
        Project p = MAPPER.readValue(json, Project.class);
        assertInstanceOf(Project.class, p);
    }

    // Same, but for the property-based-creator + `@JsonUnwrapped` path
    @Test
    public void currentValueNotClobberedByUnwrappedCreator() throws Exception {
        String json = """
                { "source": { "id": "s1", "first": "Bob", "last": "Smith" },
                  "assets": [ { "name": "a1" } ] }
                """;
        ProjectWithUnwrapped p = MAPPER.readValue(json, ProjectWithUnwrapped.class);
        assertInstanceOf(ProjectWithUnwrapped.class, p);
    }
}

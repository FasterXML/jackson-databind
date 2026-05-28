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

    // Captures `currentValue()` seen while deserializing the `assets` property,
    // so each test can positively assert the expected enclosing type.
    static Object CURRENT_VALUE;

    // Consume the whole array first (pops stream context back to enclosing object),
    // then capture currentValue -- as the reporter's deserializer does.
    static class AssetDeserializer extends ValueDeserializer<List<Asset>> {
        @Override
        public List<Asset> deserialize(JsonParser p, DeserializationContext ctxt) {
            p.readValueAsTree();
            CURRENT_VALUE = p.currentValue();
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
        CURRENT_VALUE = null;
        String json = """
                { "source": { "id": "s1" }, "assets": [ { "name": "a1" } ] }
                """;
        MAPPER.readValue(json, Project.class);
        assertInstanceOf(Project.class, CURRENT_VALUE);
    }

    // Same, but for the property-based-creator + `@JsonUnwrapped` path
    @Test
    public void currentValueNotClobberedByUnwrappedCreator() throws Exception {
        CURRENT_VALUE = null;
        String json = """
                { "source": { "id": "s1", "first": "Bob", "last": "Smith" },
                  "assets": [ { "name": "a1" } ] }
                """;
        MAPPER.readValue(json, ProjectWithUnwrapped.class);
        assertInstanceOf(ProjectWithUnwrapped.class, CURRENT_VALUE);
    }
}

package tools.jackson.databind.deser;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5980]: property-based-creator deserialization of an earlier property
// must not clobber the enclosing value's `JsonParser.currentValue()`
public class CurrentValue5980Test extends DatabindTestUtil
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

    static class Asset {
        public String name;
    }

    static class AssetDeserializer extends ValueDeserializer<List<Asset>> {
        @Override
        public List<Asset> deserialize(JsonParser p, DeserializationContext ctxt) {
            // Consume the whole array first (pops stream context back to enclosing object),
            // then look at currentValue -- as the reporter's deserializer does.
            p.readValueAsTree();
            Object cv = p.currentValue();
            assertNotNull(cv, "currentValue() should be the enclosing Project, was null");
            assertInstanceOf(Project.class, cv,
                    "currentValue() should be Project, was: " + cv.getClass().getName());
            return List.of();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void currentValueNotClobberedByCreator() throws Exception {
        String json = """
                { "source": { "id": "s1" }, "assets": [ { "name": "a1" } ] }
                """;
        Project p = MAPPER.readValue(json, Project.class);
        assertNotNull(p);
    }
}

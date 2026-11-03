package tools.jackson.databind;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5710]: ObjectWriter.valueToTree() generates incomplete tree model
// for polymorphic types when wrapped in container/reference types
class ObjectWriterValueToTree5710Test extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "subtype", value = Subtype.class)
    })
    interface Supertype {}

    @JsonTypeName("subtype")
    static class Subtype implements Supertype {
        public String content = "hello";
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Direct polymorphic type: valueToTree() should include "@type" property
    @Test
    void valueToTreeDirectPolymorphicType() throws Exception
    {
        ObjectWriter writer = MAPPER.writerFor(new TypeReference<Supertype>() {});
        Subtype value = new Subtype();

        JsonNode tree = writer.valueToTree(value);

        assertTrue(tree.has("@type"), "Expected '@type' property in tree for direct polymorphic type");
        assertEquals("subtype", tree.get("@type").asString());
        assertEquals("hello", tree.get("content").asString());
    }

    // List-wrapped polymorphic type: valueToTree() should include "@type" in array elements
    @Test
    void valueToTreeListWrappedPolymorphicType() throws Exception
    {
        ObjectWriter writer = MAPPER.writerFor(new TypeReference<List<Supertype>>() {});
        List<Subtype> value = List.of(new Subtype());

        // writeValueAsString() works correctly
        String json = writer.writeValueAsString(value);
        assertTrue(json.contains("\"@type\""), "writeValueAsString() should include '@type': " + json);

        // valueToTree() should match writeValueAsString()
        assertEquals(MAPPER.readTree(json), writer.valueToTree(value),
                "valueToTree() should match writeValueAsString() for List<Supertype>");
    }

    // Optional-wrapped polymorphic type: valueToTree() should include "@type" property
    @Test
    void valueToTreeOptionalWrappedPolymorphicType() throws Exception
    {
        ObjectWriter writer = MAPPER.writerFor(new TypeReference<Optional<Supertype>>() {});
        Optional<Subtype> value = Optional.of(new Subtype());

        // writeValueAsString() works correctly
        String json = writer.writeValueAsString(value);
        assertTrue(json.contains("\"@type\""), "writeValueAsString() should include '@type': " + json);

        // valueToTree() should match writeValueAsString()
        assertEquals(MAPPER.readTree(json), writer.valueToTree(value),
                "valueToTree() should match writeValueAsString() for Optional<Supertype>");
    }
}

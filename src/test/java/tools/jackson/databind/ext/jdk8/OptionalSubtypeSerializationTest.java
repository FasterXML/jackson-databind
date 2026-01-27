package tools.jackson.databind.ext.jdk8;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for <a href="https://github.com/FasterXML/jackson-databind/issues/5616">
 * [databind#5616]: ObjectWriter Serializes Optionals with SubTypes Incompletely
 * </a>
 * <p>
 * When serializing an {@code Optional<Supertype>} with a {@code TypeReference},
 * subtype-specific properties should be included in the output.
 */
public class OptionalSubtypeSerializationTest extends DatabindTestUtil
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Supertype.Subtype.class, name = "subtype"),
    })
    interface Supertype {
        class Subtype implements Supertype {
            public String content = "hello";
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5616]: List<Supertype> works correctly (baseline test)
    @Test
    public void testListWithSubtypeProperties() throws Exception
    {
        Supertype object = new Supertype.Subtype();

        String json = MAPPER.writerFor(new TypeReference<List<Supertype>>() {})
                .writeValueAsString(List.of(object));

        // This works: subtype property "content" is included
        assertEquals("[{\"@type\":\"subtype\",\"content\":\"hello\"}]", json);
    }

    // [databind#5616]: Optional<Supertype> loses subtype properties
    @Test
    public void testOptionalWithSubtypeProperties() throws Exception
    {
        Supertype object = new Supertype.Subtype();

        String json = MAPPER.writerFor(new TypeReference<Optional<Supertype>>() {})
                .writeValueAsString(Optional.of(object));

        // This fails: actual output is '{"@type":"subtype"}' - missing "content" property
        assertEquals("{\"@type\":\"subtype\",\"content\":\"hello\"}", json);
    }

    // Additional test: direct subtype serialization works
    @Test
    public void testDirectSubtypeSerialization() throws Exception
    {
        Supertype.Subtype object = new Supertype.Subtype();

        String json = MAPPER.writeValueAsString(object);

        // Direct serialization includes all properties
        assertEquals("{\"@type\":\"subtype\",\"content\":\"hello\"}", json);
    }

    // Additional test: Optional without TypeReference uses runtime type
    @Test
    public void testOptionalWithoutTypeReference() throws Exception
    {
        Supertype object = new Supertype.Subtype();

        // When not using TypeReference, it serializes based on actual runtime type
        // which is Subtype, so @type is not needed (not serializing as Supertype)
        String json = MAPPER.writeValueAsString(Optional.of(object));

        // Serializes as Subtype directly (no @type discriminator needed)
        assertEquals("{\"content\":\"hello\"}", json);
    }
}

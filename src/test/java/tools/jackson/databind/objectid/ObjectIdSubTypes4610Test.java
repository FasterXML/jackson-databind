package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectIdSubTypes4610Test extends DatabindTestUtil
{
    // Unused @JsonIdentityInfo
    @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EnumTypeDefinition.class, name = "enum"),
            @JsonSubTypes.Type(value = NumberTypeDefinition.class, name = "number")
    })
    interface TypeDefinition {
    }

    static class EnumTypeDefinition implements TypeDefinition {
        public List<String> values;
    }

    static class NumberTypeDefinition implements TypeDefinition {
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void shouldHandleTypeDefinitionJson() throws Exception {
        String input = "{\"@type\": \"number\"}";

        TypeDefinition model = MAPPER.readerFor(TypeDefinition.class)
                .without(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .readValue(input);

        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }

    @Test
    public void testRoundTrip() throws Exception {
        // Ser
        String JSON = MAPPER.writeValueAsString(new NumberTypeDefinition());
        assertTrue(JSON.contains("@id"));

        // Deser
        TypeDefinition model = MAPPER.readerFor(TypeDefinition.class)
                .with(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                .readValue(JSON);
        Assertions.assertInstanceOf(NumberTypeDefinition.class, model);
    }

    @Test
    public void shouldHandleTypeDefinitionJsonFail() throws Exception {
        String JSON = "{\"@type\": \"number\"}";

        try {
            /*TypeDefinition model =*/ MAPPER.readerFor(TypeDefinition.class)
                    .with(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS)
                    .readValue(JSON);
            fail("Should not pass");
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("No Object Id found for an instance of"));
        }
    }
}


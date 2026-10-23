package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5861]: Allow mapping of absent `JsonNode` to `MissingNode`
public class AbsentNodeToMissing5861Test extends DatabindTestUtil
{
    static class CreatorBean5861
    {
        public final JsonNode node;

        @JsonCreator
        public CreatorBean5861(@JsonProperty("node") JsonNode n) {
            this.node = n;
        }
    }

    private final ObjectMapper DEFAULT_MAPPER = newJsonMapper();

    private final ObjectMapper MISSING_MAPPER = jsonMapperBuilder()
            .enable(JsonNodeFeature.MAP_ABSENT_TO_MISSING)
            .build();

    // Verify default behavior: absent -> null (backwards compatible)
    @Test
    public void testAbsentCreatorDefaultGivesNull() throws Exception
    {
        CreatorBean5861 result = DEFAULT_MAPPER.readValue("{}", CreatorBean5861.class);
        assertNull(result.node);
    }

    // With MAP_ABSENT_TO_MISSING enabled: absent -> MissingNode
    @Test
    public void testAbsentCreatorGivesMissingNode() throws Exception
    {
        CreatorBean5861 result = MISSING_MAPPER.readValue("{}", CreatorBean5861.class);
        assertNotNull(result.node);
        assertTrue(result.node.isMissingNode(),
                "Expected MissingNode, got: " + result.node.getClass().getSimpleName());
    }

    // Explicit null should still give NullNode regardless of feature
    @Test
    public void testExplicitNullStillGivesNullNode() throws Exception
    {
        CreatorBean5861 result = MISSING_MAPPER.readValue("{\"node\":null}", CreatorBean5861.class);
        assertNotNull(result.node);
        assertTrue(result.node.isNull(),
                "Expected NullNode, got: " + result.node.getClass().getSimpleName());
    }

    // Explicit value should work normally regardless of feature
    @Test
    public void testExplicitValueUnaffected() throws Exception
    {
        CreatorBean5861 result = MISSING_MAPPER.readValue("{\"node\":42}", CreatorBean5861.class);
        assertNotNull(result.node);
        assertTrue(result.node.isNumber());
        assertEquals(42, result.node.intValue());
    }
}

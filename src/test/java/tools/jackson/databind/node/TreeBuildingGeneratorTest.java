package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

public class TreeBuildingGeneratorTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();
    
    // For [databind#5528]
    @Test
    void testNumberAsString()
    {
        try (JsonGenerator g = _generator()) {
            g.writeStartArray();
            try {
                g.writeNumber("123");
                fail("Should not pass");
            } catch (StreamWriteException e) {
                verifyException(e, "TreeBuildingGenerator` does not support `writeNumber(String)`, must write Numbers as typed");
            }
        }
        
    }

    TreeBuildingGenerator _generator() {
        return TreeBuildingGenerator.forSerialization(null, MAPPER.getNodeFactory());
    }
}
